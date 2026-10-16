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
package org.apache.juneau.petstore.console.dev;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Dashboard: the dashboard-chrome demo page, built with {@link DevPageBuilders#dashboard(String, int)} only.
 *
 * <p>
 * {@code ?variant=} picks how the {@code orders} source behaves ({@code ok}, {@code slow}, {@code truncated} or
 * {@code error}; {@code right} behaves as {@code ok} and puts the rail on the right) and {@code ?v=} sets the persist version.  {@code /series/<variant>} serves one CSV row per UTC day in
 * {@code [start, end)}, filtered by {@code reasons}, {@code q} and {@code lateOnly}; {@code /requests} counts the series
 * requests, so a test can see Refresh and Clear cache reach the server.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	GET /console/dev/dashboard?variant=slow
 * 	GET /console/dev/dashboard/series/ok?start=2026-10-01T00:00:00Z&amp;end=2026-10-08T00:00:00Z&amp;reasons=late
 * </p>
 */
@Rest(path="/dashboard", title="Dashboard")
@SuppressWarnings({
	"java:S110", // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
	"resource" // The response writer is owned by the servlet container; Eclipse JDT @Owning warning is by design.
})
public class DashboardRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** Series requests served since startup, for the Refresh and Clear cache checks. */
	static final AtomicInteger SERIES_REQUESTS = new AtomicInteger();

	private static final List<String> VARIANTS = List.of("ok", "slow", "truncated", "error", "right");
	private static final List<String> REASONS = List.of("late", "lost", "damaged", "wrong-item");
	private static final String[] WEEKDAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
	private static final LocalDate FIXED_START = LocalDate.of(2026, 9, 1);
	private static final int FIXED_DAYS = 28;
	private static final int TRUNCATE_AT = 20;
	private static final long SLOW_MS = 2000;
	private static final Duration MAX_WINDOW = Duration.ofDays(400);

	/**
	 * Renders the Dashboard page.
	 *
	 * @param req The request.
	 * @param variant The source variant; anything unknown is {@code ok}.
	 * @param v The persist version; below 1 (or absent) is 1.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req, @Query("variant") String variant, @Query("v") Integer v) {
		return twin(req, DevPageBuilders.dashboard(variant != null && VARIANTS.contains(variant) ? variant : "ok", v == null || v < 1 ? 1 : v));
	}

	/**
	 * The {@code orders} source: {@code day,reason,orders}, one row per UTC day.
	 *
	 * @param req The request (for the repeated {@code reasons} parameter).
	 * @param res The response.
	 * @param variant {@code ok}, {@code slow} (half the rows, a pause, the rest), {@code truncated} (at most 20 rows and
	 * 	the {@code #truncated,20} sentinel) or {@code error} (a 500).
	 * @param q Keeps rows whose reason contains this text, case-insensitively; empty keeps every row.
	 * @param start The window start, ISO-8601; defaults to 30 days before {@code end}.
	 * @param end The window end, ISO-8601, exclusive; defaults to the start of tomorrow (UTC).
	 * @param lateOnly {@code true} keeps only {@code late}.
	 * @throws IOException If the response can't be written.
	 */
	@RestGet(path="/series/{variant}")
	public void series(RestRequest req, RestResponse res, @Path("variant") String variant, @Query("q") @Schema(aev=true) String q,
			@Query("start") String start, @Query("end") String end, @Query("lateOnly") String lateOnly) throws IOException {
		SERIES_REQUESTS.incrementAndGet();
		if ("error".equals(variant))
			throw new InternalServerError("The demo source failed on purpose (variant=error).");
		var reasons = new HashSet<String>();
		for (var p : req.getQueryParams().getAll("reasons"))
			reasons.add(p.getValue());
		var e = instant(end, LocalDate.now(ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
		var s = instant(start, e.minus(Duration.ofDays(30)));
		if (s.isBefore(e.minus(MAX_WINDOW)))
			s = e.minus(MAX_WINDOW);
		var rows = rows(s, e, reasons, q == null || q.isEmpty() ? null : q.toLowerCase(Locale.ROOT), "true".equals(lateOnly));
		var truncated = "truncated".equals(variant) && rows.size() > TRUNCATE_AT;
		if (truncated)
			rows = rows.subList(0, TRUNCATE_AT);
		res.setContentType("text/csv;charset=utf-8");
		res.setHeader("X-Content-Type-Options", "nosniff");
		var w = res.getWriter();
		w.write("day,reason,orders\n");
		var half = "slow".equals(variant) ? rows.size() / 2 : rows.size();
		for (var i = 0; i < rows.size(); i++) {
			if (i == half && i > 0) {
				w.flush();
				res.flushBuffer();
				try {
					Thread.sleep(SLOW_MS);
				} catch (InterruptedException x) {
					Thread.currentThread().interrupt();
					return;
				}
			}
			w.write(rows.get(i));
		}
		if (truncated)
			w.write("#truncated," + TRUNCATE_AT + "\n");
	}

	/**
	 * Orders by reason over a fixed 28 days, for the {@code by-reason} bar and the {@code share} donut.
	 *
	 * @return One row per reason.
	 */
	@RestGet(path="/reasons")
	public List<JsonMap> reasons() {
		var totals = new LinkedHashMap<String,Integer>();
		REASONS.forEach(r -> totals.put(r, 0));
		for (var i = 0; i < FIXED_DAYS; i++) {
			var n = FIXED_START.plusDays(i).toEpochDay();
			totals.merge(reason(n), orders(n), Integer::sum);
		}
		var l = new ArrayList<JsonMap>();
		totals.forEach((r, o) -> l.add(JsonMap.of("reason", r, "orders", o)));
		return l;
	}

	/**
	 * Orders by weekday over the same fixed 28 days, for the {@code by-weekday} bar.
	 *
	 * @return One row per weekday, Monday first.
	 */
	@RestGet(path="/weekdays")
	public List<JsonMap> weekdays() {
		var totals = new int[7];
		for (var i = 0; i < FIXED_DAYS; i++) {
			var day = FIXED_START.plusDays(i);
			totals[day.getDayOfWeek().getValue() - 1] += orders(day.toEpochDay());
		}
		var l = new ArrayList<JsonMap>();
		for (var i = 0; i < 7; i++)
			l.add(JsonMap.of("weekday", WEEKDAYS[i], "orders", totals[i]));
		return l;
	}

	/**
	 * The series request counter.
	 *
	 * @return {@code {series: n}}.
	 */
	@RestGet(path="/requests")
	public JsonMap requests() {
		return JsonMap.of("series", SERIES_REQUESTS.get());
	}

	/**
	 * Serves the page-local script (the {@code petstore.late} rail section).
	 *
	 * @param res The response.
	 * @throws IOException If the bundled script can't be read.
	 */
	@RestGet(path="/dashboard.js")
	public void script(RestResponse res) throws IOException {
		try (var in = DashboardRest.class.getResourceAsStream("dashboard.js")) {
			if (in == null)
				throw new IllegalStateException("Missing classpath resource 'dashboard.js' beside " + DashboardRest.class.getName());
			res.setContentType("text/javascript;charset=utf-8");
			res.getWriter().write(new String(in.readAllBytes(), UTF_8));
		}
	}

	private static List<String> rows(Instant start, Instant end, Set<String> reasons, String q, boolean lateOnly) {
		var out = new ArrayList<String>();
		for (var day = start.atZone(ZoneOffset.UTC).toLocalDate(); day.atStartOfDay(ZoneOffset.UTC).toInstant().isBefore(end); day = day.plusDays(1)) {
			var n = day.toEpochDay();
			var r = reason(n);
			if ((reasons.isEmpty() || reasons.contains(r)) && (q == null || r.contains(q)) && (! lateOnly || "late".equals(r)))
				out.add(day.atStartOfDay(ZoneOffset.UTC).toInstant() + "," + r + "," + orders(n) + "\n");
		}
		return out;
	}

	private static String reason(long epochDay) {
		return REASONS.get((int) Math.floorMod(epochDay, 4L));
	}

	private static int orders(long epochDay) {
		return 10 + (int) Math.floorMod(epochDay * 7, 23L);
	}

	private static Instant instant(String v, Instant def) {
		if (v == null || v.isBlank())
			return def;
		try {
			return Instant.parse(v);
		} catch (DateTimeParseException x) {
			return def;
		}
	}
}
