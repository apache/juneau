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
import org.apache.juneau.http.response.*;
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
 * 	<li>Same-origin event {@code href}s to a {@code /event/{id}} detail page, so a no-JS reader can still follow a
 * 		chip.
 * 	<li>The per-month {@code /events/{year}/{month}} GET returning the {@code {contractVersion, year, month, events}}
 * 		envelope the runtime hydrates other months from.
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(mixins=ViewsMixin.class)
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
			months from <code>%s</code>. A day with more events than fit shows a &quot;+N more&quot; control.</p>
			%s
			<script src="%s"></script>
			</body>
			</html>
			""".formatted(
				ViewsMixin.viewAssetUrl(req, ViewsMixin.VIEWS_CSS_PATH),
				ViewsMixin.viewAssetUrl(req, ViewsMixin.CALENDAR_CSS_PATH),
				ENDPOINT,
				calendarMarkup,
				ViewsMixin.viewAssetUrl(req, ViewsMixin.CALENDAR_JS_PATH));
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
		for (var e : eventsFor(year, month))
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
		return out;
	}

	/** Builds one all-day event with a same-origin detail href. */
	private static CalendarEvent event(int year, int month, int day, int seq, String title, String categoryId) {
		var start = LocalDate.of(year, month, day).format(DateTimeFormatter.ISO_LOCAL_DATE);
		var id = "%04d-%02d-%02d-%d".formatted(year, month, day, seq);
		return CalendarEvent.create()
			.id(id)
			.title(title)
			.start(start)
			.categoryId(categoryId)
			.href("event/" + id);
	}

	/** The wire-envelope element shape for one event (mirrors the seed sidecar the emitter writes). */
	private static Map<String,Object> wire(CalendarEvent e) {
		var m = new LinkedHashMap<String,Object>();
		m.put("id", e.id);
		m.put("title", e.title);
		m.put("start", e.start);
		m.put("allDay", e.effectiveAllDay());
		m.put("categoryId", e.categoryId);
		m.put("href", e.href);
		return m;
	}
}
