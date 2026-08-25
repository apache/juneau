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

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.commons.http.*;

/**
 * A reusable month-calendar definition: month/year navigation, event categories, and a display-only legend
 * (parent concept #10).
 *
 * <p>
 * This is the {@link Widget} marker type &mdash; the {@code juneau-rest-server-widgets}
 * {@code org.apache.juneau.rest.server.widgets.Widget}, <b>not</b> {@code org.apache.juneau.marshall.html.HtmlWidget}
 * (the unrelated {@code $W{...}} HTML header-widget SPI).  It is <b>bean-only</b>: the emitter
 * ({@code CalendarTable}) and the {@code juneau-calendar.js}/{@code .css} runtime live in
 * {@code juneau-rest-server-views}, which composes this bean.  Widgets keeps no dependency on views.
 *
 * <p>
 * The server renders the initial month grid + legend and paints real seed chips into the day cells (progressive
 * enhancement); the runtime then computes each month client-side and hydrates events from a same-origin, data-only
 * per-month GET whose envelope is {@code {contractVersion, year, month, events}}.  When {@link #endpoint} is
 * <jk>null</jk> the calendar is seed-only and month navigation is disabled.
 *
 * @since 10.0.0
 */
public class CalendarDef implements Widget {

	/**
	 * The frozen contract version for the per-month event GET envelope and the stamped
	 * {@code data-juneau-calendar-contract} attribute.  Serialized as the JSON <b>string</b> {@code "2"} (a numeric
	 * {@code 2} would fail the client's strict {@code ===} handshake).
	 *
	 * <p>
	 * Bumped from {@code "1"} to {@code "2"} for the {@code end}-becomes-layout-significant change even though the
	 * envelope gained <b>no new required field</b> ({@code end} was already carried for forward-compat).  The bump
	 * is justified <i>because this same constant is the stamped {@code data-juneau-calendar-contract} attribute</i>:
	 * a v1 runtime paired with a v2 server would silently ignore {@code end} and paint a multi-day event as a single
	 * chip, so making the handshake fail loud is the point.  Do <b>not</b> copy this as a general pattern &mdash; a
	 * wire contract that is not also a runtime-handshake stamp should not bump for a no-new-field change.
	 */
	public static final String CONTRACT_VERSION = "2";

	/** The default chip cap per day cell before collapsing into "+N more". */
	public static final int DEFAULT_MAX_PER_DAY = 3;

	/**
	 * The hard internal ceiling on spanning-bar lanes in one week row.
	 *
	 * <p>
	 * The lane budget is <b>derived</b> from {@link #effectiveMaxPerDay()} and clamped here; it is deliberately not
	 * author-controllable (there is no {@code maxLanesPerWeek} bean field).  Bars that would need a lane beyond the
	 * budget are collapsed into the day cells' "+N more" affordance instead of stacking a week row arbitrarily tall.
	 */
	public static final int MAX_LANES_PER_WEEK = 8;

	/** The charset every id (calendar / category) must match. */
	private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]*");

	/** Required, non-blank instance id; charset {@code [A-Za-z][A-Za-z0-9_-]*}.  Emitted as {@code data-juneau-calendar}. */
	public String id;

	/**
	 * Same-origin path template for the per-month event GET; must contain {@code {year}} and {@code {month}}.
	 * May be <jk>null</jk>: seed-only, with month navigation disabled.
	 */
	public String endpoint;

	/** Declared event categories, in legend display order.  A referenced {@code categoryId} must be declared here. */
	public List<EventCategory> categories;

	/** Optional initial-month seed events, painted into the initial month server-side and embedded as the sidecar. */
	public List<CalendarEvent> events;

	/** Which view to render; v1 supports {@link CalendarView#MONTH} only.  Defaults to {@code MONTH}. */
	public CalendarView view;

	/** First column of the week grid; defaults to {@link WeekStart#SUNDAY}. */
	public WeekStart weekStart;

	/** Optional 1-based initial month ({@code 1..12}); paired with {@link #initialYear}. */
	public Integer initialMonth;

	/** Optional initial year; paired with {@link #initialMonth}. */
	public Integer initialYear;

	/** Optional max chips per day before "+N more"; defaults to {@link #DEFAULT_MAX_PER_DAY}. */
	public Integer maxPerDay;

	/**
	 * Creates an empty calendar definition.
	 *
	 * @return A new {@link CalendarDef}.
	 */
	public static CalendarDef create() {
		return new CalendarDef();
	}

	/**
	 * Sets the instance id.
	 *
	 * @param value The id.  Must match {@code [A-Za-z][A-Za-z0-9_-]*}.
	 * @return This object.
	 */
	public CalendarDef id(String value) {
		id = value;
		return this;
	}

	/**
	 * Sets the per-month event GET template.
	 *
	 * @param value A same-origin path template containing {@code {year}} and {@code {month}}, or <jk>null</jk> for
	 * 	seed-only.
	 * @return This object.
	 */
	public CalendarDef endpoint(String value) {
		endpoint = value;
		return this;
	}

	/**
	 * Sets the declared categories, in legend order.
	 *
	 * @param value The categories.
	 * @return This object.
	 */
	public CalendarDef categories(EventCategory...value) {
		categories = l(value);
		return this;
	}

	/**
	 * Sets the initial-month seed events.
	 *
	 * @param value The seed events.
	 * @return This object.
	 */
	public CalendarDef events(CalendarEvent...value) {
		events = l(value);
		return this;
	}

	/**
	 * Sets the view.
	 *
	 * @param value The view.
	 * @return This object.
	 */
	public CalendarDef view(CalendarView value) {
		view = value;
		return this;
	}

	/**
	 * Sets the week start column.
	 *
	 * @param value The week start.
	 * @return This object.
	 */
	public CalendarDef weekStart(WeekStart value) {
		weekStart = value;
		return this;
	}

	/**
	 * Sets the initial year and 1-based month together.
	 *
	 * @param year The initial year.
	 * @param month The 1-based initial month ({@code 1..12}).
	 * @return This object.
	 */
	public CalendarDef initial(int year, int month) {
		initialYear = year;
		initialMonth = month;
		return this;
	}

	/**
	 * Sets the max chips per day before "+N more".
	 *
	 * @param value The cap; must be {@code >= 1}.
	 * @return This object.
	 */
	public CalendarDef maxPerDay(int value) {
		maxPerDay = value;
		return this;
	}

	/**
	 * The effective view &mdash; the declared {@link #view} or {@link CalendarView#MONTH} when unset.
	 *
	 * @return The effective view.
	 */
	public CalendarView effectiveView() {
		return view == null ? CalendarView.MONTH : view;
	}

	/**
	 * The effective week start &mdash; the declared {@link #weekStart} or {@link WeekStart#SUNDAY} when unset.
	 *
	 * @return The effective week start.
	 */
	public WeekStart effectiveWeekStart() {
		return weekStart == null ? WeekStart.SUNDAY : weekStart;
	}

	/**
	 * The effective chip cap &mdash; the declared {@link #maxPerDay} or {@link #DEFAULT_MAX_PER_DAY} when unset.
	 *
	 * @return The effective chip cap.
	 */
	public int effectiveMaxPerDay() {
		return maxPerDay == null ? DEFAULT_MAX_PER_DAY : maxPerDay;
	}

	/**
	 * The per-week-row spanning-bar lane budget &mdash; {@link #effectiveMaxPerDay()} clamped to
	 * {@link #MAX_LANES_PER_WEEK}.
	 *
	 * <p>
	 * Spanning bars occupy <b>lanes</b> counted against this budget; {@link #maxPerDay} itself continues to cap the
	 * <b>non-spanning</b> chips (single-day all-day plus timed) inside a day cell.  The two caps are separate on
	 * purpose: a bar crossing a cell does not consume that cell's chip budget, and a hidden chip does not consume a
	 * lane.
	 *
	 * @return The derived lane budget, at least 1 and at most {@link #MAX_LANES_PER_WEEK}.
	 */
	public int effectiveLaneBudget() {
		return Math.min(effectiveMaxPerDay(), MAX_LANES_PER_WEEK);
	}

	/**
	 * The seed events that are well-formed enough to paint &mdash; the malformed ones are dropped, exactly as a
	 * per-month GET handler drops them via {@link CalendarEvent#retainWellFormed(Collection)}.
	 *
	 * @return A new mutable list of paintable seed events, in declaration order.  Never <jk>null</jk>.
	 */
	public List<CalendarEvent> wellFormedEvents() {
		return CalendarEvent.retainWellFormed(events);
	}

	/** The declared category ids. */
	private Set<String> categoryIds() {
		var ids = new HashSet<String>();
		if (categories != null)
			for (var c : categories)
				if (c != null && c.id != null)
					ids.add(c.id);
		return ids;
	}

	@Override /* Widget */
	public void validate() {
		validate(initialYear, initialMonth);
	}

	/**
	 * Fail-closed bean validation against a resolved (year, month) window.
	 *
	 * <p>
	 * When {@link #initialYear}/{@link #initialMonth} are unset, the emitter resolves the effective window from the
	 * injected clock (F1) and passes it here so seed events are still checked against the month actually rendered.
	 *
	 * @param effectiveYear The year the initial month is rendered for, or <jk>null</jk> to skip the off-month check.
	 * @param effectiveMonth The 1-based month rendered, or <jk>null</jk> to skip the off-month check.
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate(Integer effectiveYear, Integer effectiveMonth) {
		if (id == null || id.isBlank())
			throw iaex("CalendarDef id must not be null or blank.");
		if (!ID_PATTERN.matcher(id).matches())
			throw iaex("CalendarDef id '%s' must match [A-Za-z][A-Za-z0-9_-]*.", id);

		if (endpoint != null) {
			if (endpoint.isBlank())
				throw iaex("CalendarDef endpoint must not be blank (use null for a seed-only calendar).");
			if (!SafePathTemplate.isSafeTemplate(endpoint, "{year}", "{month}"))
				throw iaex("CalendarDef endpoint must be a same-origin path template containing {year} and {month}: %s",
					endpoint);
		}

		if (maxPerDay != null && maxPerDay < 1)
			throw iaex("CalendarDef maxPerDay must be >= 1.");

		if ((initialMonth == null) != (initialYear == null))
			throw iaex("CalendarDef initialMonth and initialYear must be set together or neither.");
		if (initialMonth != null && (initialMonth < 1 || initialMonth > 12))
			throw iaex("CalendarDef initialMonth must be in 1..12.");

		var seenCat = new HashSet<String>();
		if (categories != null) {
			for (var c : categories) {
				if (c == null)
					throw iaex("CalendarDef category must not be null.");
				if (c.id == null || c.id.isBlank())
					throw iaex("EventCategory id must not be null or blank.");
				if (!ID_PATTERN.matcher(c.id).matches())
					throw iaex("EventCategory id '%s' must match [A-Za-z][A-Za-z0-9_-]*.", c.id);
				if (!seenCat.add(c.id))
					throw iaex("CalendarDef duplicate category id '%s'.", c.id);
				if (c.label == null || c.label.isBlank())
					throw iaex("EventCategory '%s' label must not be null or blank.", c.id);
			}
		}

		if (events != null) {
			var known = categoryIds();
			var seenEvt = new HashSet<String>();
			for (var e : events) {
				// A malformed event is DROPPED, not fatal: one bad event must never cost the whole calendar.  The
				// per-month GET path drops the same set via CalendarEvent.retainWellFormed.
				if (e == null || e.malformedReason() != null)
					continue;
				if (!seenEvt.add(e.id))
					throw iaex("CalendarDef duplicate event id '%s'.", e.id);
				e.validate(known, effectiveYear, effectiveMonth);
			}
		}
	}

	/** The view families; v1 ships {@code MONTH} only so an author cannot set a value that only fails at {@code validate()}. */
	public enum CalendarView {

		/** The month grid view. */
		MONTH
	}

	/** The first column of the week grid. */
	public enum WeekStart {

		/** Weeks start on Sunday (the default). */
		SUNDAY,

		/** Weeks start on Monday. */
		MONDAY;

		/** The lowercased wire token ({@code sunday}/{@code monday}). */
		public String token() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
