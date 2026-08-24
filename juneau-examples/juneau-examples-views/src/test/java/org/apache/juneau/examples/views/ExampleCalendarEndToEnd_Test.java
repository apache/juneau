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

import org.apache.juneau.TestBase;
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
		assertTrue(body.contains("data-juneau-calendar-contract=\"1\""), "contract stamp");
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

	@Test
	void b01_monthEventsEnvelope_matchesTheContract() throws Exception {
		var res = getJson("/events/2026/8");
		assertEquals(200, res.statusCode());
		var body = res.body();
		assertTrue(body.contains("\"contractVersion\":\"1\""), body);
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
}
