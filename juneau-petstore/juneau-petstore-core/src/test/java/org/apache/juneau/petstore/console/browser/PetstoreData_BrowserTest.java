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
package org.apache.juneau.petstore.console.browser;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.petstore.console.browser.PetstoreBrowser.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.extension.*;

/**
 * The Data sources page in the real browser: the day-chunked source loads four chunks and draws them, and a reload
 * requests none of them, because closed days come from IndexedDB.  The window is pinned to four past days, so the
 * run does not depend on the clock.
 */
@EnabledIfSystemProperty(named=PetstoreBrowser.GATE, matches="true", disabledReason=PetstoreBrowser.DISABLED)
class PetstoreData_BrowserTest extends TestBase {

	@RegisterExtension
	static MicroserviceTestFixture server = PetstoreTestServer.fixture();

	private static final String READY =
		"(() => { const h = JuneauViews.bus.get('data:orders'); return h && h.status === 'ready'; })()";
	private static final String ROWS = "JuneauViews.data.get(JuneauViews.bus.get('data:orders')).rowCount";

	/** The pinned window end (exclusive): the window is the four UTC days before it, all closed. */
	private static final Instant END = Instant.parse("2026-10-01T00:00:00Z");

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void probe() throws Exception {
		var base = PetstoreTestServer.awaitReady(server);
		report = run("data", base, List.of(
			Map.of("name", "data", "path", "/console/dev/data?end=2026-10-01", "capture", "/console/dev/data/orders",
				"actions", List.of(
					Map.of("waitFor", READY, "timeoutMs", 15000),
					Map.of("reload", true),
					Map.of("waitFor", READY, "timeoutMs", 15000)),
				"queries", Map.of(
					"range", "JuneauViews.bus.get('petstore.range') != null",
					"rows", ROWS,
					"cache", "JuneauViews.bus.get('data:orders').cache",
					"echarts", "document.querySelectorAll('canvas').length > 0"))));
	}

	@SuppressWarnings({
		"unchecked" // requests is a list of {url, method, postData}.
	})
	private static List<String> urls(Map<String,Object> c) {
		return ((List<Map<String,Object>>) c.get("requests")).stream().map(r -> (String) r.get("url")).toList();
	}

	private static String day(Instant start) {
		var end = URLEncoder.encode(start.plus(1, ChronoUnit.DAYS).toString(), UTF_8);
		return "end=" + end + "&start=" + URLEncoder.encode(start.toString(), UTF_8);
	}

	@Test void a01_firstLoadRequestsExactlyTheFourDayChunks() {
		var u = urls(assertClean(report, "data"));
		var got = new TreeSet<String>();
		u.subList(0, Math.min(4, u.size())).forEach(x -> got.add(x.substring(x.indexOf('?') + 1)));
		var want = new TreeSet<String>();
		for (var i = 4; i >= 1; i--)
			want.add(day(END.minus(i, ChronoUnit.DAYS)));
		assertEquals(want, got, () -> "requests: " + u);
		assertTrue(u.size() >= 4, () -> "requests: " + u);
	}

	@Test void a02_reloadRequestsNoClosedDay() {
		var u = urls(assertClean(report, "data"));
		assertSize(() -> "requests: " + u, 4, u);
	}

	@Test void a03_reloadDrawsTheSameRowsFromTheCache() {
		var c = assertClean(report, "data");
		assertBean(c.get("queries"), "range,rows,cache,echarts", "true,96,indexedDb,true");
	}
}
