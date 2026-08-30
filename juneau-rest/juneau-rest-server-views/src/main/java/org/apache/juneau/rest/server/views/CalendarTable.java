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

import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.commons.utils.StringUtils.escapeForScript;

import java.time.*;
import java.time.format.*;
import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.widgets.*;
import org.apache.juneau.rest.server.widgets.CalendarDef.*;
import org.apache.juneau.rest.server.widgets.EventCategory.*;

/**
 * Builds the HTML delivery shell for a {@link CalendarDef} &mdash; the {@code data-juneau-calendar} month grid with
 * its spanning bars and chips, the toggle-filter legend, the day-cell and event-chip {@code <template>} skeletons,
 * and an optional {@code <script type="application/json" data-juneau-calendar-seed>} sidecar the
 * {@code juneau-calendar.js} runtime consumes for the initial month.
 *
 * <h5 class='section'>Spanning bars, chips, and the two caps:</h5>
 * <p>
 * {@link CalendarLayout} owns the geometry: a multi-day event is drawn as a bar cut into one piece per week row
 * (every piece carrying the same {@code data-juneau-calendar-event-id} so hover, focus, and the legend filter act on
 * the whole event), while single-day events are chips inside their day cell.  Bars consume per-week lanes against
 * {@link CalendarDef#effectiveLaneBudget()}; {@link CalendarDef#maxPerDay} caps only the chips.  A timed chip
 * carries a leading {@code HH:mm} label and sorts after the day's all-day chips.
 *
 * <h5 class='section'>Legend toggle-filter:</h5>
 * <p>
 * Each legend entry is a real {@code <button aria-pressed>} so the category filter is keyboard-reachable before any
 * script runs; the runtime wires the presses and filters client-side with no refetch.  A no-JS reader sees the
 * legend as a plain, all-pressed list.
 *
 * <p>
 * This is the views-module emitter (parent decision L5 B / i1 A): {@link CalendarDef} and friends are
 * <b>bean-only</b> in {@code juneau-rest-server-widgets}; views <b>composes</b> them here.  Widgets keeps no
 * dependency on views.  The renderer paints the initial month's real day cells <b>and</b> real event chips for the
 * seed events (true progressive enhancement &mdash; a no-JS reader sees a populated month with same-origin event
 * links as plain anchors), then emits the {@code <template>} skeletons the runtime clones for other months.
 *
 * <h5 class='section'>Clock / civil-today (design doc &sect;5.7, l2):</h5>
 * <p>
 * "Today" and the default month come from an injected {@link Clock} &mdash; never a field on the bean and never
 * {@code Date.now()} in the runtime's pure code.  The civil today date is stamped onto the root as
 * {@code data-juneau-calendar-today="yyyy-MM-dd"} (from {@link LocalDate#now(Clock)}, honoring the clock's zone;
 * the default zone policy is UTC).  When {@link CalendarDef#initialYear}/{@link CalendarDef#initialMonth} are unset
 * the emitter resolves the effective window from the clock and passes it to
 * {@link CalendarDef#validate(Integer, Integer)} so seed events are still checked against the month actually
 * rendered.
 *
 * <h5 class='section'>Escaping contract (security-critical &mdash; mirrors {@link ViewTable}):</h5>
 * <p>
 * The seed sidecar JSON is emitted as the raw-text body of a {@code <script type="application/json">} element and is
 * therefore handed to {@link org.apache.juneau.commons.utils.StringUtils#escapeForScript(String)} before insertion
 * &mdash; a title containing {@code </script>} cannot break out of the element.  JSON is <b>data only</b>: the
 * runtime fills every title/tooltip/label with {@code textContent}, never {@code innerHTML}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link CalendarDef}
 * 	<li class='jc'>{@link ViewTable}
 * </ul>
 *
 * @since 10.0.0
 */
public class CalendarTable {

	/** Marker attribute the {@code juneau-calendar.js} runtime looks for; value = the instance {@link CalendarDef#id}. */
	public static final String MARKER_ATTR = "data-juneau-calendar";

	/** Attribute carrying the baked contract-version string; the runtime fails loud on mismatch. */
	public static final String CONTRACT_ATTR = "data-juneau-calendar-contract";

	/** Attribute carrying the server-stamped civil today {@code yyyy-MM-dd} (from the injected clock). */
	public static final String TODAY_ATTR = "data-juneau-calendar-today";

	/** Attribute carrying the same-origin {@code {year}}/{@code {month}} GET template.  Absent when seed-only. */
	public static final String ENDPOINT_ATTR = "data-juneau-calendar-endpoint";

	/** Attribute carrying the view token ({@code month} in v1). */
	public static final String VIEW_ATTR = "data-juneau-calendar-view";

	/** Attribute carrying the week-start token ({@code sunday}/{@code monday}). */
	public static final String WEEKSTART_ATTR = "data-juneau-calendar-weekstart";

	/** Attribute carrying the integer chip cap before "+N more" (non-spanning chips only). */
	public static final String MAXPERDAY_ATTR = "data-juneau-calendar-maxperday";

	/** Attribute carrying the derived per-week spanning-bar lane budget (never author-set; see {@link CalendarDef#effectiveLaneBudget()}). */
	public static final String LANEBUDGET_ATTR = "data-juneau-calendar-lanebudget";

	/** Attribute carrying an event's id on every chip and on every piece of its spanning bar. */
	public static final String EVENT_ID_ATTR = "data-juneau-calendar-event-id";

	/** Marker attribute on a legend entry's {@code aria-pressed} category toggle. */
	public static final String LEGEND_TOGGLE_ATTR = "data-juneau-calendar-legend-toggle";

	/** Marker attribute on the spanning-bar {@code <template>} skeleton. */
	public static final String BAR_TEMPLATE_ATTR = "data-juneau-calendar-bar";

	/** Marker attribute on the nav {@code <div>} wrapping the prev/next/today buttons. */
	public static final String NAV_ATTR = "data-juneau-calendar-nav";

	/** Marker attribute on the {@code aria-live} month/year title element. */
	public static final String TITLE_ATTR = "data-juneau-calendar-title";

	/** Marker attribute on the {@code role="grid"} month-grid mount. */
	public static final String GRID_ATTR = "data-juneau-calendar-grid";

	/** Marker attribute on the legend {@code <ul>}. */
	public static final String LEGEND_ATTR = "data-juneau-calendar-legend";

	/** Attribute carrying a category id on a legend item and on each painted chip (drives the color class). */
	public static final String CAT_ATTR = "data-juneau-calendar-cat";

	/** Marker attribute on the day-cell {@code <template>} skeleton. */
	public static final String DAY_TEMPLATE_ATTR = "data-juneau-calendar-day";

	/** Marker attribute on the event-chip {@code <template>} skeleton. */
	public static final String EVENT_TEMPLATE_ATTR = "data-juneau-calendar-event";

	/** Marker attribute on the optional {@code <script type="application/json">} seed sidecar. */
	public static final String SEED_ATTR = "data-juneau-calendar-seed";

	/** English weekday abbreviations when weeks start on Sunday. */
	private static final String[] WEEKDAYS_SUNDAY = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

	/** English weekday abbreviations when weeks start on Monday. */
	private static final String[] WEEKDAYS_MONDAY = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

	/** The {@code aria-label} attribute name, shared by several emit helpers below. */
	private static final String ARIA_LABEL = "aria-label";

	/** The HTML5 builder's {@code "button"} type token, shared by several emit helpers below. */
	private static final String TYPE_BUTTON = "button";

	/** The shared HTML {@code title} attribute / seed-envelope {@code title} field name. */
	private static final String TITLE_KEY = "title";

	private CalendarTable() {}

	/**
	 * Builds the calendar shell using the system-UTC clock (test/convenience; the civil today is then UTC-based).
	 *
	 * @param def The built calendar definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the month grid, legend, {@code <template>}s, and optional seed sidecar.
	 */
	public static Div of(CalendarDef def) {
		return of(def, Clock.systemUTC());
	}

	/**
	 * Builds the calendar shell for the specified definition and clock.
	 *
	 * <p>
	 * Resolves the effective (year, month) window &mdash; the bean's {@link CalendarDef#initialYear}/
	 * {@link CalendarDef#initialMonth} when set, else the clock's civil year/month &mdash; validates the bean against
	 * that window (fail-closed on the serving path), then paints the initial month.
	 *
	 * @param def The built calendar definition.  Must not be <jk>null</jk>.
	 * @param clock The clock supplying civil today and the default month.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the month grid, legend, {@code <template>}s, and optional seed sidecar.
	 */
	public static Div of(CalendarDef def, Clock clock) {
		return of(def, clock, Locale.ENGLISH);
	}

	/**
	 * Builds the calendar shell for the current request, localizing the month/year title against
	 * {@link RestRequest#getLocale()} (honors {@code Accept-Language}).
	 *
	 * <p>
	 * Everything else &mdash; weekday abbreviations, ARIA labels, seed events &mdash; is unaffected by this locale;
	 * only the month/year title text (e.g. {@code "August 2026"}) localizes.  Uses the system-UTC clock; see
	 * {@link #of(RestRequest, CalendarDef, Clock)} to supply a different one.
	 *
	 * @param req The current request, whose {@link RestRequest#getLocale()} supplies the title locale.  Must not be
	 * 	<jk>null</jk>.
	 * @param def The built calendar definition.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the month grid, legend, {@code <template>}s, and optional seed sidecar.
	 */
	public static Div of(RestRequest req, CalendarDef def) {
		return of(req, def, Clock.systemUTC());
	}

	/**
	 * Builds the calendar shell for the current request and the specified clock, localizing the month/year title
	 * against {@link RestRequest#getLocale()} (honors {@code Accept-Language}).
	 *
	 * @param req The current request, whose {@link RestRequest#getLocale()} supplies the title locale.  Must not be
	 * 	<jk>null</jk>.
	 * @param def The built calendar definition.  Must not be <jk>null</jk>.
	 * @param clock The clock supplying civil today and the default month.  Must not be <jk>null</jk>.
	 * @return A new {@link Div} carrying the month grid, legend, {@code <template>}s, and optional seed sidecar.
	 */
	public static Div of(RestRequest req, CalendarDef def, Clock clock) {
		return of(def, clock, req.getLocale());
	}

	/** The shared core of the {@code of(...)} overloads above; {@code locale} governs only the month/year title. */
	private static Div of(CalendarDef def, Clock clock, Locale locale) {
		var today = LocalDate.now(clock);
		var year = def.initialYear != null ? def.initialYear : today.getYear();
		var month = def.initialMonth != null ? def.initialMonth : today.getMonthValue();

		// Fail-closed on the serving path against the window actually rendered (mirrors the 445a B1 non-vacuous fix).
		def.validate(year, month);

		var weekStart = def.effectiveWeekStart();
		var maxPerDay = def.effectiveMaxPerDay();

		var root = div(
			header(year, month, def.endpoint != null, locale),
			grid(def, year, month, weekStart, today),
			legend(def),
			dayTemplate(),
			eventTemplate(),
			barTemplate()
		).class_("jc-cal").attr("role", "group").attr(ARIA_LABEL, "Calendar");

		root.attr(MARKER_ATTR, def.id);
		root.attr(CONTRACT_ATTR, CalendarDef.CONTRACT_VERSION);
		root.attr(TODAY_ATTR, today.format(DateTimeFormatter.ISO_LOCAL_DATE));
		if (def.endpoint != null)
			root.attr(ENDPOINT_ATTR, def.endpoint);
		root.attr(VIEW_ATTR, def.effectiveView().name().toLowerCase(Locale.ROOT));
		root.attr(WEEKSTART_ATTR, weekStart.token());
		root.attr(MAXPERDAY_ATTR, String.valueOf(maxPerDay));
		root.attr(LANEBUDGET_ATTR, String.valueOf(def.effectiveLaneBudget()));

		var seed = seedSidecar(def, year, month);
		if (seed != null)
			root.children(seed);
		return root;
	}

	/** Prev/next/today nav (disabled when seed-only) plus the {@code aria-live} month/year title. */
	private static Div header(int year, int month, boolean nav, Locale locale) {
		var prev = button(TYPE_BUTTON, "‹").attr("data-juneau-calendar-prev", "1")
			.attr(ARIA_LABEL, "Previous month").class_("jc-cal-nav-btn");
		var next = button(TYPE_BUTTON, "›").attr("data-juneau-calendar-next", "1")
			.attr(ARIA_LABEL, "Next month").class_("jc-cal-nav-btn");
		var todayBtn = button(TYPE_BUTTON, "Today").attr("data-juneau-calendar-today-btn", "1")
			.attr(ARIA_LABEL, "Today").class_("jc-cal-nav-btn jc-cal-today-btn");
		if (!nav) {
			prev.disabled(true);
			next.disabled(true);
			todayBtn.disabled(true);
		}
		return div(
			div(prev, next, todayBtn).attr(NAV_ATTR, "1").class_("jc-cal-nav"),
			div(monthTitle(year, month, locale)).attr(TITLE_ATTR, "1").attr("aria-live", "polite").class_("jc-cal-title")
		).class_("jc-cal-header");
	}

	/** The month grid: a weekday header row plus six week rows of day cells with painted seed bars and chips. */
	private static Div grid(CalendarDef def, int year, int month, WeekStart weekStart, LocalDate today) {
		var layout = CalendarLayout.of(def, year, month);

		var headerCells = new ArrayList<>(7);
		for (var name : weekStart == WeekStart.MONDAY ? WEEKDAYS_MONDAY : WEEKDAYS_SUNDAY)
			headerCells.add(span(name).attr("role", "columnheader").class_("jc-cal-weekday"));
		var headerRow = div(headerCells.toArray()).attr("role", "row").class_("jc-cal-weekdays");

		var rows = new ArrayList<>(7);
		rows.add(headerRow);
		for (var w = 0; w < CalendarLayout.GRID_WEEKS; w++) {
			var lanes = layout.laneCount(w);
			var barsByColumn = new HashMap<Integer,List<CalendarLayout.Segment>>();
			for (var s : layout.segmentsForWeek(w))
				barsByColumn.computeIfAbsent(s.startColumn(), k -> new ArrayList<>()).add(s);

			var cells = new ArrayList<>(7);
			for (var d = 0; d < CalendarLayout.WEEK_DAYS; d++) {
				var date = layout.gridStart().plusDays((long) w * CalendarLayout.WEEK_DAYS + d);
				var inMonth = date.getMonthValue() == month && date.getYear() == year;
				cells.add(dayCell(def, date, inMonth, date.equals(today), layout.day(date), lanes,
					barsByColumn.get(d)));
			}
			rows.add(div(cells.toArray()).attr("role", "row").class_("jc-cal-week"));
		}
		return div(rows.toArray()).attr(GRID_ATTR, "1").attr("role", "grid").class_("jc-cal-grid");
	}

	/** One painted day cell: the day number, the bar pieces starting here, the capped chips, and "+N more". */
	private static Div dayCell(CalendarDef def, LocalDate date, boolean inMonth, boolean isToday,
			CalendarLayout.DayCell cell, int lanes, List<CalendarLayout.Segment> bars) {
		var cls = "jc-cal-day";
		if (!inMonth)
			cls += " jc-cal-day--adjacent";
		if (isToday)
			cls += " jc-cal-day--today";

		var kids = new ArrayList<>();
		kids.add(span(String.valueOf(date.getDayOfMonth())).class_("jc-cal-day-num"));

		// Every cell of a week row reserves the same lane band so the bars line up across the row.
		if (lanes > 0)
			kids.add(laneBand(def, bars));
		kids.add(dayEvents(def, inMonth, cell));

		var el = div(kids.toArray()).attr("role", "gridcell").class_(cls);
		if (lanes > 0)
			el.attr("style", "--jc-cal-lanes:" + lanes);
		if (isToday)
			el.attr("aria-current", "date");
		return el;
	}

	/** The lane band reserved by every cell of a week row, painted with any bar pieces starting in this cell. */
	private static Div laneBand(CalendarDef def, List<CalendarLayout.Segment> bars) {
		var pieces = new ArrayList<>();
		if (bars != null)
			for (var s : bars)
				pieces.add(bar(def, s));
		return div(pieces.toArray()).class_("jc-cal-day-lanes");
	}

	/** The capped event chips for one in-month day cell, plus a "+N more" chip when the day overflows the cap. */
	private static Div dayEvents(CalendarDef def, boolean inMonth, CalendarLayout.DayCell cell) {
		var chips = new ArrayList<>();
		if (inMonth && cell != null) {
			for (var e : cell.chips())
				chips.add(chip(def, e));
			if (cell.overflow() > 0)
				chips.add(button(TYPE_BUTTON, "+" + cell.overflow() + " more")
					.attr("data-juneau-calendar-more", "1").class_("jc-cal-more"));
		}
		return div(chips.toArray()).class_("jc-cal-day-events");
	}

	/** One painted piece of a spanning bar, anchored in the day cell where the piece starts. */
	private static HtmlElement<?> bar(CalendarDef def, CalendarLayout.Segment s) {
		var e = s.event();
		var cls = new StringBuilder("jc-cal-bar jc-cal-cat--").append(colorToken(def, e.categoryId));
		if (s.continuesLeft())
			cls.append(" jc-cal-bar--continues-left");
		if (s.continuesRight())
			cls.append(" jc-cal-bar--continues-right");
		var el = anchorOrSpan(e, cls.toString(), null, e.title);
		el.attr("style", "--jc-cal-span:" + s.columnSpan() + ";--jc-cal-lane:" + s.lane());
		el.attr(EVENT_ID_ATTR, e.id);
		if (s.continuesLeft() || s.continuesRight())
			el.attr(ARIA_LABEL, e.title + " (continues)");
		decorate(el, e);
		return el;
	}

	/** One painted event chip; a timed chip carries a leading {@code HH:mm} label ahead of its title. */
	private static HtmlElement<?> chip(CalendarDef def, CalendarEvent e) {
		var cls = "jc-cal-event jc-cal-cat--" + colorToken(def, e.categoryId);
		var time = e.startTimeLabel();
		if (time != null)
			cls += " jc-cal-event--timed";
		var el = anchorOrSpan(e, cls, time, e.title);
		el.attr(EVENT_ID_ATTR, e.id);
		decorate(el, e);
		return el;
	}

	/** A same-origin anchor when {@code href} is a safe document URL, else a span; with an optional time label. */
	private static HtmlElement<?> anchorOrSpan(CalendarEvent e, String cls, String time, String title) {
		Object[] body = time == null
			? new Object[]{title}
			: new Object[]{span(time).class_("jc-cal-event-time"), span(title).class_("jc-cal-event-title")};
		var linked = e.href != null && !e.href.isBlank() && SafePathTemplate.isSafeDocumentUrl(e.href);
		HtmlElement<?> el = linked ? a(e.href, body) : span(body);
		return el.class_(cls);
	}

	/** Stamps the category hook and the tooltip shared by chips and bar pieces. */
	private static void decorate(HtmlElement<?> el, CalendarEvent e) {
		if (e.categoryId != null && !e.categoryId.isBlank())
			el.attr(CAT_ATTR, e.categoryId);
		if (e.tooltip != null && !e.tooltip.isBlank())
			el.attr(TITLE_KEY, e.tooltip);
	}

	/** The legend: one {@code aria-pressed} category toggle per declared category, in declared order. */
	private static Ul legend(CalendarDef def) {
		var items = new ArrayList<>();
		if (def.categories != null) {
			for (var c : def.categories) {
				var toggle = button(TYPE_BUTTON,
					span().class_("jc-cal-legend-swatch"), span(c.label).class_("jc-cal-legend-label"))
					.attr(LEGEND_TOGGLE_ATTR, "1")
					.attr(CAT_ATTR, c.id)
					.attr("aria-pressed", "true")
					.class_("jc-cal-legend-toggle");
				if (c.description != null && !c.description.isBlank())
					toggle.attr(TITLE_KEY, c.description);
				items.add(li(toggle)
					.attr(CAT_ATTR, c.id)
					.class_("jc-cal-legend-item jc-cal-cat--" + c.effectiveColor().token()));
			}
		}
		return ul(items.toArray()).attr(LEGEND_ATTR, "1").class_("jc-cal-legend");
	}

	/** The day-cell skeleton the runtime clones for every cell of a non-seed month. */
	private static Template dayTemplate() {
		return template().attr(DAY_TEMPLATE_ATTR, "1").children(
			div(
				span().class_("jc-cal-day-num"),
				div().class_("jc-cal-day-lanes"),
				div().class_("jc-cal-day-events")
			).attr("role", "gridcell").class_("jc-cal-day"));
	}

	/** The event-chip skeleton the runtime clones and fills with {@code textContent}. */
	private static Template eventTemplate() {
		return template().attr(EVENT_TEMPLATE_ATTR, "1").children(
			span().class_("jc-cal-event"));
	}

	/** The spanning-bar skeleton the runtime clones for a navigated month's bar pieces. */
	private static Template barTemplate() {
		return template().attr(BAR_TEMPLATE_ATTR, "1").children(
			span().class_("jc-cal-bar"));
	}

	/** The optional {@code escapeForScript}-encoded initial-month seed envelope, or {@code null} when no seed events. */
	private static Script seedSidecar(CalendarDef def, int year, int month) {
		// Only the events the server actually painted: a malformed one is dropped here too, so the sidecar the
		// runtime rehydrates from can never disagree with the server-painted month.
		var wellFormed = def.wellFormedEvents();
		if (wellFormed.isEmpty())
			return null;
		var events = new ArrayList<java.util.Map<String,Object>>();
		for (var e : wellFormed)
			events.add(eventMap(e));
		var envelope = new LinkedHashMap<String,Object>();
		envelope.put("contractVersion", CalendarDef.CONTRACT_VERSION);
		envelope.put("year", year);
		envelope.put("month", month);
		envelope.put("events", events);
		var json = escapeForScript(Json.of(envelope));
		return script().type("application/json").attr(SEED_ATTR, "1").text(rawText(json));
	}

	/** Serializes one seed event to the wire envelope's element shape, omitting unset optional fields. */
	private static java.util.Map<String,Object> eventMap(CalendarEvent e) {
		var m = new LinkedHashMap<String,Object>();
		m.put("id", e.id);
		m.put(TITLE_KEY, e.title);
		m.put("start", e.start);
		if (e.end != null)
			m.put("end", e.end);
		m.put("allDay", e.effectiveAllDay());
		if (e.categoryId != null)
			m.put("categoryId", e.categoryId);
		if (e.href != null)
			m.put("href", e.href);
		if (e.tooltip != null)
			m.put("tooltip", e.tooltip);
		return m;
	}

	/** The color token for an event's category, defaulting to {@code neutral} for an unknown/absent category. */
	private static String colorToken(CalendarDef def, String categoryId) {
		if (categoryId != null && def.categories != null)
			for (var c : def.categories)
				if (categoryId.equals(c.id))
					return c.effectiveColor().token();
		return CategoryColor.NEUTRAL.token();
	}

	/** The month/year title in the given locale, e.g. {@code "August 2026"} for {@link Locale#ENGLISH}. */
	private static String monthTitle(int year, int month, Locale locale) {
		return Month.of(month).getDisplayName(TextStyle.FULL, locale) + " " + year;
	}
}
