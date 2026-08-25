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

import static org.junit.jupiter.api.Assertions.*;

import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Proves the calendar example actually works: boots {@link ExampleCalendarServer} in-process on an ephemeral port
 * and drives real HTTP requests at it &mdash; the server-painted current-month page, the per-month events envelope,
 * an event detail page, and the first-party calendar CSS/JS assets it links to.
 */
@SuppressWarnings({
	"resource" // server is opened in @BeforeAll/closed in @AfterAll.
})
class ExampleCalendarEndToEnd_Test extends TestBase {

	private static ExampleCalendarServer server;
	private static HttpClient http;

	@BeforeAll
	static void startServer() throws Exception {
		server = ExampleCalendarServer.start(0);
		http = HttpClient.newHttpClient();
	}

	@AfterAll
	static void stopServer() throws Exception {
		if (server != null)
			server.close();
	}

	private HttpResponse<String> get(String path) throws Exception {
		var uri = server.getRootUrl().resolve(path);
		var req = HttpRequest.newBuilder(uri).GET().build();
		return http.send(req, BodyHandlers.ofString());
	}

	private HttpResponse<String> getJson(String path) throws Exception {
		var uri = server.getRootUrl().resolve(path);
		var req = HttpRequest.newBuilder(uri).header("Accept", "application/json").GET().build();
		return http.send(req, BodyHandlers.ofString());
	}

	@Test
	void a01_indexPage_paintsCurrentMonthServerSide_withHooksLegendAndSeedChips() throws Exception {
		var res = get("/");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("data-juneau-calendar=\"team-calendar\""), "calendar marker");
		assertTrue(body.contains("data-juneau-calendar-contract=\"" + CalendarDef.CONTRACT_VERSION + "\""),
			"contract stamp");
		assertTrue(body.contains("data-juneau-calendar-endpoint=\"/events/{year}/{month}\""), "endpoint stamp");
		assertTrue(body.contains("role=\"grid\""), "server-painted grid");
		// Progressive enhancement: real seed chips painted before any JS.
		assertTrue(body.contains("Sprint planning"), "seed chip painted");
		// The busy day (4 events, cap 3) shows the overflow control.
		assertTrue(body.contains("data-juneau-calendar-more"), "+N more control");
		// Legend lists the four declared categories with their color classes.
		assertTrue(body.contains("jc-cal-cat--blue"), "team color");
		assertTrue(body.contains("jc-cal-cat--red"), "incident color");
	}

	@Test
	void a02_indexPage_assetLinks_areResolvedFetchableUrls() throws Exception {
		var body = get("/").body();
		assertFalse(body.contains("servlet:"), "no unresolved servlet: URIs");
		assertTrue(body.contains("href=\"/juneau-calendar.css?v="), "resolved calendar.css URL");
		assertTrue(body.contains("src=\"/juneau-calendar.js?v="), "resolved calendar.js URL");
	}

	/**
	 * The load-order contract: the calendar's "+N more" popover is a layer on the ONE shared stack that
	 * {@code juneau-views.js} publishes, so the views script tag must come first or the calendar fails loud.
	 */
	@Test
	void a03_viewsScriptIsLoadedBeforeTheCalendarScript() throws Exception {
		var body = get("/").body();
		var views = body.indexOf("src=\"/juneau-views.js");
		var calendar = body.indexOf("src=\"/juneau-calendar.js");
		assertTrue(views >= 0, "the page must load juneau-views.js");
		assertTrue(calendar >= 0, "the page must load juneau-calendar.js");
		assertTrue(views < calendar, "juneau-views.js must be loaded BEFORE juneau-calendar.js");
	}

	/** The legend is a row of real {@code aria-pressed} toggles now, not display-only chrome. */
	@Test
	void a04_legendEntriesAreAriaPressedToggles() throws Exception {
		var body = get("/").body();
		assertTrue(body.contains("data-juneau-calendar-legend-toggle"), body);
		assertTrue(body.contains("aria-pressed=\"true\""), body);
	}

	/** The example dogfoods the new layout: a multi-day span paints as a segmented bar, timed chips carry a label. */
	@Test
	void a05_indexPage_paintsSpanningBarsAndTimedChips() throws Exception {
		var body = get("/").body();
		assertTrue(body.contains("jc-cal-bar"), "a spanning bar is painted");
		assertTrue(body.contains("jc-cal-bar--continues-right") || body.contains("jc-cal-bar--continues-left"),
			"the span crosses a week boundary, so a cut edge is flagged");
		assertTrue(body.contains("jc-cal-event--timed"), "a timed chip is painted");
		assertTrue(body.contains("jc-cal-event-time"), "a timed chip carries its HH:mm label");
	}

	@Test
	void b01_monthEventsEnvelope_matchesTheContract() throws Exception {
		var res = getJson("/events/2026/8");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("\"contractVersion\":\"" + CalendarDef.CONTRACT_VERSION + "\""), body);
		assertTrue(body.contains("\"year\":2026"), body);
		assertTrue(body.contains("\"month\":8"), body);
		assertTrue(body.contains("\"Sprint planning\""), body);
		assertTrue(body.contains("\"categoryId\":\"team\""), body);
		assertTrue(body.contains("\"start\":\"2026-08-15\""), body);
		assertTrue(body.contains("\"href\":\"event/2026-08-15-1\""), body);
	}

	@Test
	void b02_monthEventsEnvelope_clampsBusyDayToShortMonth() throws Exception {
		// February always has a 15th, so the busy day is still present in a short month.
		var res = getJson("/events/2026/2");
		assertEquals(200, res.statusCode());
		assertTrue(res.body().contains("\"month\":2"), res.body());
	}

	@Test
	void c01_eventDetailPage_isReachable() throws Exception {
		var res = get("/event/2026-08-15-1");
		assertEquals(200, res.statusCode());
		assertTrue(res.body().contains("2026-08-15-1"), res.body());
	}

	@Test
	void d01_calendarJsAsset_isReachable() throws Exception {
		var res = get("/juneau-calendar.js");
		assertEquals(200, res.statusCode());
		assertTrue(res.body().contains("JuneauCalendar"), "ships the calendar runtime namespace");
	}

	@Test
	void d02_calendarCssAsset_isReachable() throws Exception {
		var res = get("/juneau-calendar.css");
		assertEquals(200, res.statusCode());
		assertTrue(res.body().contains("jc-cal"), res.body());
	}

	/**
	 * The host composes both the views and the widget mixin (a server that failed to boot with both would have
	 * already failed every test above), and now that the calendar's bytes have been relocated, the page resolves
	 * them through the <b>widget</b> mixin that ships them.
	 */
	@Test
	void e01_hostComposesBothMixins_andCalendarAssetsResolveThroughWidgets() throws Exception {
		var mixins = List.of(ExampleCalendarRest.class.getAnnotation(Rest.class).mixins());
		assertTrue(mixins.contains(ViewsMixin.class), mixins::toString);
		assertTrue(mixins.contains(WidgetsMixin.class), mixins::toString);

		// The links on the page carry the widget module's version+content-hash cache-buster, and are fetchable.
		var body = get("/").body();
		var expected = "?v=" + WidgetsMixin.widgetAssetUrl(WidgetsMixin.CALENDAR_JS_PATH).split("\\?v=")[1];
		assertTrue(body.contains("src=\"/juneau-calendar.js" + expected + "\""), body);
	}

	/**
	 * Composing both mixins is harmless because the views mixin's mount for a relocated asset is a compatibility
	 * mount reading the widget module's bytes: the two mixins hand out the same URL, so the page cannot end up
	 * caching one script under two busters.
	 */
	@Test
	@SuppressWarnings("deprecation") // The point of the test is that the deprecated views URL has not diverged.
	void e02_bothMixins_handOutTheSameCalendarAssetUrls() {
		assertEquals(WidgetsMixin.widgetAssetUrl(WidgetsMixin.CALENDAR_JS_PATH), ViewsMixin.viewAssetUrl(ViewsMixin.CALENDAR_JS_PATH));
		assertEquals(WidgetsMixin.widgetAssetUrl(WidgetsMixin.CALENDAR_CSS_PATH), ViewsMixin.viewAssetUrl(ViewsMixin.CALENDAR_CSS_PATH));
	}
}
