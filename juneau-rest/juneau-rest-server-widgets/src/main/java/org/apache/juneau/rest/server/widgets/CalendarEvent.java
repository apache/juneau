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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

import org.apache.juneau.commons.http.*;

/**
 * A single calendar event &mdash; also the element type of the per-month event GET envelope's {@code events}.
 *
 * <p>
 * Bean-only ({@code juneau-rest-server-widgets}): the emitter and runtime live in {@code juneau-rest-server-views}.
 * Titles, tooltips, and labels are painted with {@code textContent} by the runtime, never as markup.
 *
 * <h5 class='section'>{@code end} is layout-significant, with split inclusivity:</h5>
 * <ul>
 * 	<li><b>All-day (date-only {@code end}):</b> <b>inclusive</b>.  {@code start=2026-03-02}, {@code end=2026-03-04}
 * 		occupies <b>three</b> day cells.
 * 	<li><b>Timed (date-time {@code end}):</b> <b>exclusive</b>.  {@code start=…T09:00}, {@code end=…T10:00} occupies
 * 		<code>[09:00, 10:00)</code> &mdash; the start day only.  A timed event crossing midnight is a spanning bar
 * 		using the exclusive {@code end}, never intra-day chips on each crossed day.
 * 	<li><b>Omitted {@code end}:</b> start-only, and <b>valid</b>.  An all-day event occupies the start day cell; a
 * 		timed event occupies the start instant as a zero-width chip on the start day (never a spanning bar).
 * 	<li>An all-day {@code end} equal to {@code start} is a single-day event; a <b>timed</b> {@code end} equal to
 * 		{@code start} is zero-duration and is rejected, as is any {@code end} before {@code start}.
 * </ul>
 *
 * <h5 class='section'>All-day agreement and the malformed set:</h5>
 * <p>
 * Agreement between {@code allDay} and the shape of {@code end} is decided by {@link #effectiveAllDay()}, never by
 * the raw {@link #allDay} field, so a <jk>null</jk> {@code allDay} paired with matching {@code start}/{@code end}
 * shapes stays valid.  An event is <b>malformed</b> for exactly these reasons &mdash; and no others:
 * <ol>
 * 	<li>a missing/blank {@link #id}, {@link #title}, or {@link #start};
 * 	<li>an unparseable {@code start}/{@code end} civil date, or an {@code end} that is not after (all-day: not on or
 * 		after) {@code start};
 * 	<li>a declared {@code allDay}/{@code end} disagreement &mdash; {@code allDay == } <jk>true</jk> with a date-time
 * 		{@code end}, or {@code allDay == } <jk>false</jk> with a date-only {@code end};
 * 	<li>mixed {@code start}/{@code end} shapes with a <jk>null</jk> {@code allDay}.
 * </ol>
 * <p>
 * A malformed event is <b>dropped</b> by {@link CalendarDef#validate()} and by a per-month GET handler that filters
 * through {@link #retainWellFormed(Collection)} &mdash; the same rule on both paths.  Neither fails the whole
 * definition nor the whole month payload.  An omitted {@code end} is <b>not</b> malformed.
 *
 * <h5 class='section'>Timezone contract:</h5>
 * <p>
 * {@code start}/{@code end} are interpreted as an ISO-8601 local date ({@code yyyy-MM-dd}) or local date-time
 * ({@code yyyy-MM-ddTHH:mm}).  A trailing offset or {@code Z} is <b>fail-soft ignored, never rejected</b> &mdash; a
 * seeded {@code 2026-03-02T09:00:00Z} keeps validating and keeps rendering at 09:00 in the server's injected clock
 * zone.  The same parse applies on the seed path and on the per-month GET path.
 *
 * @since 10.0.0
 */
public class CalendarEvent {

	/** Stable id; required, non-blank.  Used for de-duplication and coalesce keys. */
	public String id;

	/** Required, non-blank display title; painted with {@code textContent}. */
	public String title;

	/** Required ISO-8601 date ({@code yyyy-MM-dd}) or date-time; the civil date drives chip placement. */
	public String start;

	/**
	 * Optional ISO-8601 date/date-time bound.  Layout-significant: <b>inclusive</b> for an all-day (date-only)
	 * value, <b>exclusive</b> for a timed (date-time) value.  Omitting it is valid and means start-only.
	 */
	public String end;

	/** Optional all-day flag; defaults to <jk>true</jk> when {@code start} is a date with no time (see {@link #effectiveAllDay()}). */
	public Boolean allDay;

	/** Optional category id; must reference a declared {@link EventCategory} for a seed event; unknown wire ids paint {@code NEUTRAL}. */
	public String categoryId;

	/** Optional same-origin <b>document</b> URL ({@link SafePathTemplate#isSafeDocumentUrl(String)} semantics). */
	public String href;

	/** Optional tooltip; painted with {@code textContent}. */
	public String tooltip;

	/**
	 * Creates an empty event.
	 *
	 * @return A new {@link CalendarEvent}.
	 */
	public static CalendarEvent create() {
		return new CalendarEvent();
	}

	/**
	 * Sets the stable id.
	 *
	 * @param value The id.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 */
	public CalendarEvent id(String value) {
		id = value;
		return this;
	}

	/**
	 * Sets the display title.
	 *
	 * @param value The title.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 */
	public CalendarEvent title(String value) {
		title = value;
		return this;
	}

	/**
	 * Sets the ISO-8601 start.
	 *
	 * @param value A {@code yyyy-MM-dd} date or ISO date-time.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public CalendarEvent start(String value) {
		start = value;
		return this;
	}

	/**
	 * Sets the optional ISO-8601 end.
	 *
	 * @param value A {@code yyyy-MM-dd} date or ISO date-time; must be {@code >= start} when present.
	 * @return This object.
	 */
	public CalendarEvent end(String value) {
		end = value;
		return this;
	}

	/**
	 * Sets the all-day flag.
	 *
	 * @param value The all-day flag.
	 * @return This object.
	 */
	public CalendarEvent allDay(boolean value) {
		allDay = value;
		return this;
	}

	/**
	 * Sets the category id.
	 *
	 * @param value The category id.
	 * @return This object.
	 */
	public CalendarEvent categoryId(String value) {
		categoryId = value;
		return this;
	}

	/**
	 * Sets the same-origin document href.
	 *
	 * @param value The href.
	 * @return This object.
	 */
	public CalendarEvent href(String value) {
		href = value;
		return this;
	}

	/**
	 * Sets the tooltip.
	 *
	 * @param value The tooltip.
	 * @return This object.
	 */
	public CalendarEvent tooltip(String value) {
		tooltip = value;
		return this;
	}

	/**
	 * The effective all-day flag: the declared {@link #allDay} when set, otherwise <jk>true</jk> when {@code start}
	 * is a date with no time component.
	 *
	 * @return The effective all-day flag.
	 */
	public boolean effectiveAllDay() {
		if (allDay != null)
			return allDay;
		return start != null && !start.contains("T");
	}

	/**
	 * The civil date the chip paints on &mdash; the {@code yyyy-MM-dd} prefix of {@code start} (a date-time
	 * {@code start} is coerced to its leading civil date; a trailing offset is ignored).
	 *
	 * @return The civil start date.
	 * @throws IllegalArgumentException If {@code start} is null or not a parseable ISO date/date-time.
	 */
	public LocalDate civilStart() {
		return parseCivil(start, "start");
	}

	/**
	 * The civil date of {@code end}, or <jk>null</jk> when {@code end} is omitted.
	 *
	 * <p>
	 * This is the raw civil date of the bound, <b>before</b> inclusivity is applied &mdash; use {@link #lastDay()}
	 * for the last day cell the event actually occupies.
	 *
	 * @return The civil end date, or <jk>null</jk> when {@code end} is omitted or blank.
	 * @throws IllegalArgumentException If {@code end} is present but not a parseable ISO date/date-time.
	 */
	public LocalDate civilEnd() {
		return hasEnd() ? parseCivil(end, "end") : null;
	}

	/**
	 * The last day cell this event occupies, applying the split inclusivity rule (see the class javadoc).
	 *
	 * <p>
	 * Equals {@link #civilStart()} when {@code end} is omitted.  For an all-day event the date-only {@code end} is
	 * inclusive; for a timed event the date-time {@code end} is exclusive, so an end of exactly midnight lands on
	 * the previous day.  Never earlier than {@link #civilStart()}.
	 *
	 * @return The inclusive last occupied day.
	 * @throws IllegalArgumentException If {@code start}/{@code end} are not parseable.
	 */
	public LocalDate lastDay() {
		var startDate = civilStart();
		if (!hasEnd())
			return startDate;
		var endDate = parseCivil(end, "end");
		if (effectiveAllDay())
			return endDate.isBefore(startDate) ? startDate : endDate;
		// Exclusive bound: the last occupied instant is one nanosecond before end, so a midnight end stays on the
		// previous day.  An unparseable time degrades to midnight rather than rejecting the event.
		var last = endDate.atTime(civilTimeOrMidnight(end)).minusNanos(1).toLocalDate();
		return last.isBefore(startDate) ? startDate : last;
	}

	/**
	 * Whether this event covers more than one day cell and therefore renders as a spanning bar rather than a chip.
	 *
	 * @return <jk>true</jk> if {@link #lastDay()} is after {@link #civilStart()}.
	 * @throws IllegalArgumentException If {@code start}/{@code end} are not parseable.
	 */
	public boolean spanning() {
		return lastDay().isAfter(civilStart());
	}

	/**
	 * The leading {@code HH:mm} time label a timed chip renders, or <jk>null</jk> for an all-day event or an
	 * unparseable time.
	 *
	 * @return The {@code HH:mm} label, or <jk>null</jk>.
	 */
	public String startTimeLabel() {
		if (effectiveAllDay())
			return null;
		var t = civilTime(start);
		return t == null ? null : "%02d:%02d".formatted(t.getHour(), t.getMinute());
	}

	/**
	 * The reason this event is malformed, or <jk>null</jk> when it is well-formed.
	 *
	 * <p>
	 * This is the single drop predicate shared by {@link CalendarDef#validate()} (the seed path) and by
	 * {@link #retainWellFormed(Collection)} (the per-month GET path); see the class javadoc for the complete,
	 * closed list of malformed reasons.  It is a pure shape check: an unknown {@code categoryId}, an unsafe
	 * {@code href}, and an off-month {@code start} are author errors reported by {@link #validate(Set,Integer,Integer)},
	 * not drop reasons.
	 *
	 * @return A human-readable reason, or <jk>null</jk> when this event is well-formed.
	 */
	public String malformedReason() {
		if (id == null || id.isBlank())
			return "id must not be null or blank";
		if (title == null || title.isBlank())
			return "title must not be null or blank";
		if (civilOrNull(start) == null)
			return "start is not a valid ISO date or date-time: " + start;
		if (!hasEnd())
			return null;  // Omitted end is start-only, and valid.
		var startDate = civilOrNull(start);
		var endDate = civilOrNull(end);
		if (endDate == null)
			return "end is not a valid ISO date or date-time: " + end;
		if (effectiveAllDay() != isDateOnly(end))
			return "end shape disagrees with the effective all-day flag: " + end;
		if (effectiveAllDay())
			return endDate.isBefore(startDate) ? "end must be >= start" : null;
		var startAt = startDate.atTime(civilTimeOrMidnight(start));
		var endAt = endDate.atTime(civilTimeOrMidnight(end));
		// Exclusive end: a timed [t, t) interval is empty, so equality is rejected as well as inversion.
		return endAt.isAfter(startAt) ? null : "timed end must be after start";
	}

	/**
	 * Returns the well-formed events of the given collection, dropping the malformed ones and continuing.
	 *
	 * <p>
	 * A per-month GET handler filters its payload through this before serialization so the dynamic path drops
	 * exactly what {@link CalendarDef#validate()} drops on the seed path.  A <jk>null</jk> element is dropped too.
	 *
	 * @param values The events to filter.  May be <jk>null</jk>.
	 * @return A new mutable list of the well-formed events, in the original order.  Never <jk>null</jk>.
	 */
	public static List<CalendarEvent> retainWellFormed(Collection<CalendarEvent> values) {
		var out = new ArrayList<CalendarEvent>();
		if (values != null)
			for (var e : values)
				if (e != null && e.malformedReason() == null)
					out.add(e);
		return out;
	}

	/**
	 * Fail-closed shape check for a seed event, reused by {@link CalendarDef#validate()} and by an endpoint handler
	 * that wants to validate its own payload before serialization.
	 *
	 * <p>
	 * Throws for a malformed event (see {@link #malformedReason()}) <b>and</b> for the author errors a seed
	 * definition must not contain: an undeclared {@code categoryId}, a cross-origin {@code href}, and a
	 * {@code start} outside the rendered window.  Callers that want the drop-and-continue posture of the seed and
	 * per-month GET paths use {@link #malformedReason()}/{@link #retainWellFormed(Collection)} instead.
	 *
	 * @param knownCategoryIds The declared category ids; a set {@code categoryId} must be a member.  May be
	 * 	<jk>null</jk> (then any {@code categoryId} fails).
	 * @param initialYear The initial year the seed month is rendered for, or <jk>null</jk> to skip the off-month
	 * 	check.
	 * @param initialMonth The 1-based initial month, or <jk>null</jk> to skip the off-month check.
	 * @throws IllegalArgumentException If this event is not well-formed.
	 */
	public void validate(Set<String> knownCategoryIds, Integer initialYear, Integer initialMonth) {
		var bad = malformedReason();
		if (bad != null)
			throw iaex("CalendarEvent '%s' is malformed: %s", id, bad);
		var startDate = civilStart();
		if (categoryId != null && !categoryId.isBlank()) {
			if (knownCategoryIds == null || !knownCategoryIds.contains(categoryId))
				throw iaex("CalendarEvent '%s' categoryId '%s' is not a declared category.", id, categoryId);
		}
		if (href != null && !href.isBlank() && !SafePathTemplate.isSafeDocumentUrl(href))
			throw iaex("CalendarEvent '%s' href must be a same-origin document URL: %s", id, href);
		if (initialYear != null && initialMonth != null) {
			// Now that end is layout-significant, "in the rendered month" means the occupied range intersects it,
			// not that start falls inside it: a span beginning in the previous month is genuinely visible here.
			var monthStart = LocalDate.of(initialYear, initialMonth, 1);
			var monthEnd = monthStart.plusMonths(1).minusDays(1);
			if (lastDay().isBefore(monthStart) || startDate.isAfter(monthEnd))
				throw iaex("CalendarEvent '%s' start %s is outside the initial month %d-%02d.", id, start, initialYear,
					initialMonth);
		}
	}

	/** Whether {@code end} carries a value at all (an omitted or blank {@code end} means start-only). */
	private boolean hasEnd() {
		return end != null && !end.isBlank();
	}

	/** Whether an ISO value is date-only ({@code yyyy-MM-dd}) rather than a date-time. */
	private static boolean isDateOnly(String value) {
		return value != null && value.length() <= 10;
	}

	/** Parses the civil ({@code yyyy-MM-dd}) portion of an ISO date/date-time; never {@code Date.parse}-style zone shifting. */
	private LocalDate parseCivil(String value, String field) {
		var d = civilOrNull(value);
		if (d == null)
			throw iaex("CalendarEvent '%s' %s is not a valid ISO date or date-time: %s", id, field, value);
		return d;
	}

	/** The civil ({@code yyyy-MM-dd}) portion of an ISO date/date-time, or {@code null} when it is not parseable. */
	private static LocalDate civilOrNull(String value) {
		if (value == null || value.isBlank() || value.length() < 10)
			return null;
		if (value.length() > 10 && value.charAt(10) != 'T')
			return null;
		try {
			return LocalDate.parse(value.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * The local time of an ISO date-time, ignoring any trailing offset/{@code Z}, or {@code null} when the value is
	 * date-only or its time portion is not parseable (fail-soft: an odd time never rejects a shipped event).
	 */
	private static LocalTime civilTime(String value) {
		if (value == null || value.length() <= 11 || value.charAt(10) != 'T')
			return null;
		var body = value.substring(11);
		for (var i = 0; i < body.length(); i++) {
			var c = body.charAt(i);
			if (c == 'Z' || c == '+' || (c == '-' && i > 0)) {
				body = body.substring(0, i);
				break;
			}
		}
		try {
			return LocalTime.parse(body, DateTimeFormatter.ISO_LOCAL_TIME);
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	/** {@link #civilTime(String)} with an unparseable/absent time degraded to midnight. */
	private static LocalTime civilTimeOrMidnight(String value) {
		var t = civilTime(value);
		return t == null ? LocalTime.MIDNIGHT : t;
	}
}
