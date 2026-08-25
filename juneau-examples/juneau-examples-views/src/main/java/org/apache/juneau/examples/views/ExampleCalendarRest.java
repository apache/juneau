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
package org.apache.juneau.examples.views;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.juneau.commons.utils.CollectionUtils.list;

import java.time.*;
import java.time.format.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.Html;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.BasicRestServlet;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;
import org.apache.juneau.rest.server.widgets.EventCategory.CategoryColor;

/**
 * Dogfoods the reusable calendar widget (parent concept #10): a real caller of {@link CalendarTable#of(CalendarDef)}
 * outside the views module's own test sources, served on an embedded Jetty server via {@link ExampleCalendarServer}.
 *
 * <p>
 * The page paints the current month server-side (true progressive enhancement &mdash; the seed chips are visible
 * before any JavaScript runs, as same-origin anchors), then {@code juneau-calendar.js} hydrates prev/next/today
 * navigation by fetching other months from the same-origin {@code /events/{year}/{month}} endpoint.  The events are
 * generic synthetic entries (team syncs, reviews, releases, incident retros) so the example carries no
 * domain-specific vocabulary.
 *
 * <h5 class='section'>What this dogfoods:</h5>
 * <ul>
 * 	<li>Four declared categories mapped to four of the five {@link CategoryColor} families, exercised in the legend
 * 		and the painted chips.
 * 	<li>A day carrying four events against the default {@code maxPerDay} of three, so the "+N more" overflow control
 * 		is present.
 * 	<li>A multi-day all-day event (inclusive {@code end}) crossing a week boundary, so the spanning bar is drawn as
 * 		two continued segments, and timed events with a leading {@code HH:mm} label so chip ordering is visible.
 * 	<li>Same-origin event {@code href}s to a {@code /event/{id}} detail page, so a no-JS reader can still follow a
 * 		chip.
 * 	<li>The per-month {@code /events/{year}/{month}} GET returning the {@code {contractVersion, year, month, events}}
 * 		envelope the runtime hydrates other months from, filtered through
 * 		{@link CalendarEvent#retainWellFormed(java.util.Collection)} so the dynamic path drops exactly what the seed
 * 		path drops.
 * </ul>
 *
 * <h5 class='section'>Script load order (a contract, not a preference):</h5>
 * <p>
 * {@code juneau-views.js} is loaded <b>before</b> {@code juneau-calendar.js} because the calendar's "+N more"
 * popover registers on the <b>one shared layer stack</b> that {@code juneau-views.js} owns; the calendar defines no
 * stack of its own and fails loud when the shared one is absent.
 *
 * <h5 class='section'>Why both mixins:</h5>
 * <p>
 * The calendar's own CSS/JS now ship in the widget module, so the page resolves those two URLs through
 * {@link WidgetsMixin#widgetAssetUrl(RestRequest,String)}.  {@link ViewsMixin} is still composed because the calendar
 * is not standalone: it needs that module's base stylesheet and, above all, the shared layer stack
 * {@code juneau-views.js} publishes.
 *
 * @since 10.0.0
 */
@Rest(mixins={ViewsMixin.class, WidgetsMixin.class})
public class ExampleCalendarRest extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	/** The stable calendar instance id. */
	public static final String CALENDAR_ID = "team-calendar";

	/** The same-origin per-month events endpoint template the runtime substitutes {@code {year}}/{@code {month}} into. */
	public static final String ENDPOINT = "/events/{year}/{month}";

	private static final String CAT_TEAM = "team";
	private static final String CAT_REVIEW = "review";
	private static final String CAT_RELEASE = "release";
	private static final String CAT_INCIDENT = "incident";

	//------------------------------------------------------------------------------------------------------------------
	// The calendar definition.
	//------------------------------------------------------------------------------------------------------------------

	/** Builds the calendar definition for the given (year, month) window, seeded with that month's events. */
	static CalendarDef calendar(int year, int month) {
		return CalendarDef.create()
			.id(CALENDAR_ID)
			.endpoint(ENDPOINT)
			.categories(
				EventCategory.create().id(CAT_TEAM).label("Team").color(CategoryColor.BLUE),
				EventCategory.create().id(CAT_REVIEW).label("Reviews").color(CategoryColor.GREEN),
				EventCategory.create().id(CAT_RELEASE).label("Releases").color(CategoryColor.AMBER),
				EventCategory.create().id(CAT_INCIDENT).label("Incidents").color(CategoryColor.RED))
			.initial(year, month)
			.events(eventsFor(year, month).toArray(new CalendarEvent[0]));
	}

	//------------------------------------------------------------------------------------------------------------------
	// HTML page (hand-built, no template engine).
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * [GET /] &mdash; the current-month calendar, painted server-side, plus the first-party calendar CSS/JS the page
	 * needs.  The runtime auto-initializes the {@code data-juneau-calendar} element on load.
	 *
	 * @param req The current request, resolved against for {@link ViewsMixin#viewAssetUrl(RestRequest,String)}.
	 * @return The full HTML page.
	 */
	@RestGet(path="/", summary="The reusable-calendar demo page (current month, server-painted)")
	public HttpResource index(RestRequest req) {
		var now = LocalDate.now();
		var calendarMarkup = Html.of(CalendarTable.of(calendar(now.getYear(), now.getMonthValue())));
		var html = """
			<!DOCTYPE html>
			<html lang="en">
			<head>
			<meta charset="utf-8">
			<title>Apache Juneau - Reusable Calendar Example</title>
			<link rel="stylesheet" href="%s">
			<link rel="stylesheet" href="%s">
			<style>
			\tbody { font-family: -apple-system, Helvetica, Arial, sans-serif; margin: 2em; }
			</style>
			</head>
			<body>
			<h1>Apache Juneau &mdash; Reusable Calendar Example</h1>
			<p>The current month is painted server-side with its event chips already visible (progressive
			enhancement); <code>juneau-calendar.js</code> then hydrates prev/next/today navigation by fetching other
			months from <code>%s</code>. A day with more events than fit shows a &quot;+N more&quot; control, and the
			legend entries are toggles that filter categories client-side.</p>
			%s
			<script src="%s"></script>
			<script src="%s"></script>
			</body>
			</html>
			""".formatted(
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				WidgetsMixin.widgetAssetUrl(req, WidgetsMixin.CALENDAR_CSS_PATH),
				ENDPOINT,
				calendarMarkup,
				// juneau-views.js FIRST: it owns the shared layer stack the calendar's "+N more" popover pushes onto.
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_JS_PATH),
				WidgetsMixin.widgetAssetUrl(req, WidgetsMixin.CALENDAR_JS_PATH));
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), "text/html;charset=utf-8"),
			list(ContentType.of("text/html;charset=utf-8")));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Data endpoints.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * [GET /events/{year}/{month}] &mdash; the per-month events envelope the runtime hydrates a navigated month from.
	 *
	 * @param year The 4-digit year.
	 * @param month The 1-based month.
	 * @return {@code {contractVersion, year, month, events}} for the requested window.
	 */
	@RestGet(path="/events/{year}/{month}", swagger=@OpSwagger(ignore=true))
	public Map<String,Object> monthEvents(@Path("year") int year, @Path("month") int month) {
		var events = new ArrayList<Map<String,Object>>();
		// GET is identical to POST: a malformed event is dropped and the rest of the month still paints.  This is the
		// same predicate CalendarDef.validate() drops on, so the dynamic path can never disagree with the seed path.
		for (var e : CalendarEvent.retainWellFormed(eventsFor(year, month)))
			events.add(wire(e));
		var out = new LinkedHashMap<String,Object>();
		out.put("contractVersion", CalendarDef.CONTRACT_VERSION);
		out.put("year", year);
		out.put("month", month);
		out.put("events", events);
		return out;
	}

	/**
	 * [GET /event/{id}] &mdash; a minimal detail page for one event chip's same-origin {@code href}.
	 *
	 * @param id The event id.
	 * @return A tiny HTML page echoing the event id.
	 */
	@RestGet(path="/event/{id}", swagger=@OpSwagger(ignore=true))
	public HttpResource eventDetail(@Path("id") String id) {
		var html = """
			<!DOCTYPE html>
			<html lang="en"><head><meta charset="utf-8"><title>Event %s</title></head>
			<body><h1>Event</h1><p>Detail page for event <code>%s</code>.</p>
			<p><a href="../">Back to the calendar</a></p></body></html>
			""".formatted(id, id);
		return HttpResourceBean.of(
			ByteArrayBody.of(html.getBytes(UTF_8), "text/html;charset=utf-8"),
			list(ContentType.of("text/html;charset=utf-8")));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Generic synthetic events.
	//------------------------------------------------------------------------------------------------------------------

	/** Deterministic synthetic events for a month, clamped to the month's real length. */
	static List<CalendarEvent> eventsFor(int year, int month) {
		var len = YearMonth.of(year, month).lengthOfMonth();
		var out = new ArrayList<CalendarEvent>();
		out.add(event(year, month, Math.min(3, len), 1, "Team sync", CAT_TEAM));
		out.add(event(year, month, Math.min(8, len), 1, "Design review", CAT_REVIEW));
		// A busy day: four events against the default cap of three -> "+N more".
		var busy = Math.min(15, len);
		out.add(event(year, month, busy, 1, "Sprint planning", CAT_TEAM));
		out.add(event(year, month, busy, 2, "Roadmap review", CAT_REVIEW));
		out.add(event(year, month, busy, 3, "Release cut", CAT_RELEASE));
		out.add(event(year, month, busy, 4, "Postmortem", CAT_INCIDENT));
		out.add(event(year, month, Math.min(22, len), 1, "Patch release", CAT_RELEASE));
		out.add(event(year, month, Math.min(27, len), 1, "Incident retro", CAT_INCIDENT));
		// A nine-day all-day span (inclusive end).  Longer than a week row, so it ALWAYS cuts at a week boundary and
		// paints as continued bar segments; it also crosses the busy day, where it costs a lane, not a chip slot.
		out.add(span(year, month, Math.min(10, len), Math.min(18, len), "Release freeze", CAT_RELEASE));
		// Two timed events on the same day: they sort after that day's all-day chips, and ascend among themselves.
		out.add(timed(year, month, Math.min(3, len), "16:00", "17:00", "Retro", CAT_INCIDENT));
		out.add(timed(year, month, Math.min(3, len), "09:30", "10:30", "Standup", CAT_TEAM));
		return out;
	}

	/** Builds one all-day, single-day event with a same-origin detail href. */
	private static CalendarEvent event(int year, int month, int day, int seq, String title, String categoryId) {
		return base("%04d-%02d-%02d-%d".formatted(year, month, day, seq), title, categoryId)
			.start(day(year, month, day));
	}

	/** Builds one multi-day all-day event; the date-only {@code end} is INCLUSIVE, so both endpoints are occupied. */
	private static CalendarEvent span(int year, int month, int fromDay, int toDay, String title, String categoryId) {
		return base("%04d-%02d-%02d-span".formatted(year, month, fromDay), title, categoryId)
			.start(day(year, month, fromDay))
			.end(day(year, month, toDay));
	}

	/** Builds one timed event; the date-time {@code end} is EXCLUSIVE, so {@code [from, to)} stays on the start day. */
	private static CalendarEvent timed(int year, int month, int day, String from, String to, String title,
			String categoryId) {
		return base("%04d-%02d-%02d-%s".formatted(year, month, day, from.replace(":", "")), title, categoryId)
			.start(day(year, month, day) + "T" + from)
			.end(day(year, month, day) + "T" + to);
	}

	/** The id/title/category/href shared by every synthetic event shape. */
	private static CalendarEvent base(String id, String title, String categoryId) {
		return CalendarEvent.create().id(id).title(title).categoryId(categoryId).href("event/" + id);
	}

	/** The ISO {@code yyyy-MM-dd} civil date for a (year, month, day). */
	private static String day(int year, int month, int day) {
		return LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE);
	}

	/**
	 * The wire-envelope element shape for one event (mirrors the seed sidecar the emitter writes).
	 *
	 * <p>
	 * {@code end} was already carried by the v1 envelope for forward-compat, so making it layout-significant added
	 * <b>no new required field</b> here &mdash; only the meaning the runtime gives it changed.
	 */
	private static Map<String,Object> wire(CalendarEvent e) {
		var m = new LinkedHashMap<String,Object>();
		m.put("id", e.id);
		m.put("title", e.title);
		m.put("start", e.start);
		if (e.end != null)
			m.put("end", e.end);
		m.put("allDay", e.effectiveAllDay());
		m.put("categoryId", e.categoryId);
		m.put("href", e.href);
		return m;
	}
}
