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
 * A chip paints on the {@code start} <b>civil date only</b>; {@code end} is retained for forward-compat but is
 * <b>not</b> layout-significant in v1 (no spanning bars).  Titles, tooltips, and labels are painted with
 * {@code textContent} by the runtime, never as markup.
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

	/** Optional ISO-8601 date/date-time; must be {@code >= start} when present.  NOT layout-significant in v1. */
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
	 * The civil date the chip paints on &mdash; the {@code yyyy-MM-dd} prefix of {@code start} (S-d: a date-time
	 * {@code start} is coerced to its leading civil date; the time/offset is ignored for v1 layout).
	 *
	 * @return The civil start date.
	 * @throws IllegalArgumentException If {@code start} is null or not a parseable ISO date/date-time.
	 */
	public LocalDate civilStart() {
		return parseCivil(start, "start");
	}

	/**
	 * Fail-closed shape check for a seed event, reused by {@link CalendarDef#validate()} and by an endpoint handler
	 * that wants to validate its own payload before serialization.
	 *
	 * @param knownCategoryIds The declared category ids; a set {@code categoryId} must be a member.  May be
	 * 	<jk>null</jk> (then any {@code categoryId} fails).
	 * @param initialYear The initial year the seed month is rendered for, or <jk>null</jk> to skip the off-month
	 * 	check.
	 * @param initialMonth The 1-based initial month, or <jk>null</jk> to skip the off-month check.
	 * @throws IllegalArgumentException If this event is not well-formed.
	 */
	public void validate(Set<String> knownCategoryIds, Integer initialYear, Integer initialMonth) {
		if (id == null || id.isBlank())
			throw iaex("CalendarEvent id must not be null or blank.");
		if (title == null || title.isBlank())
			throw iaex("CalendarEvent '%s' title must not be null or blank.", id);
		var startDate = parseCivil(start, "start");
		if (end != null && !end.isBlank()) {
			var endDate = parseCivil(end, "end");
			if (endDate.isBefore(startDate))
				throw iaex("CalendarEvent '%s' end must be >= start.", id);
		}
		if (categoryId != null && !categoryId.isBlank()) {
			if (knownCategoryIds == null || !knownCategoryIds.contains(categoryId))
				throw iaex("CalendarEvent '%s' categoryId '%s' is not a declared category.", id, categoryId);
		}
		if (href != null && !href.isBlank() && !SafePathTemplate.isSafeDocumentUrl(href))
			throw iaex("CalendarEvent '%s' href must be a same-origin document URL: %s", id, href);
		if (initialYear != null && initialMonth != null) {
			if (startDate.getYear() != initialYear || startDate.getMonthValue() != initialMonth)
				throw iaex("CalendarEvent '%s' start %s is outside the initial month %d-%02d.", id, start, initialYear,
					initialMonth);
		}
	}

	/** Parses the civil ({@code yyyy-MM-dd}) portion of an ISO date/date-time; never {@code Date.parse}-style zone shifting. */
	private LocalDate parseCivil(String value, String field) {
		if (value == null || value.isBlank())
			throw iaex("CalendarEvent '%s' %s must not be null or blank.", id, field);
		var civil = value.length() >= 10 ? value.substring(0, 10) : value;
		if (value.length() > 10 && value.charAt(10) != 'T')
			throw iaex("CalendarEvent '%s' %s is not a valid ISO date or date-time: %s", id, field, value);
		try {
			return LocalDate.parse(civil, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException e) {
			throw iaex("CalendarEvent '%s' %s is not a valid ISO date or date-time: %s", id, field, value);
		}
	}
}
