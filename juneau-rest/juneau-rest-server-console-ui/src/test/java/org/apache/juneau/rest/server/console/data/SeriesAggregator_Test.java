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
package org.apache.juneau.rest.server.console.data;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SeriesAggregator_Test extends TestBase {

	static final Instant T0 = Instant.parse("2026-10-05T00:00:00Z"); // a Monday
	static final long DAY = 86_400_000L, HOUR = 3_600_000L;

	static Map<String,List<String>> q(String...kv) {
		var m = new LinkedHashMap<String,List<String>>();
		for (var i = 0; i < kv.length; i += 2)
			m.computeIfAbsent(kv[i], k -> new ArrayList<>()).add(kv[i + 1]);
		return m;
	}

	static SeriesSource.Row row(Instant t, String org, double ms) {
		var m = new HashMap<String,Object>();
		m.put("time", t);
		m.put("org", org);
		m.put("ms", ms);
		return SeriesSource.Row.of(m);
	}

	/** Hour h has org "a" when h is even, else "b", and ms = h. */
	static Stream<SeriesSource.Row> hourly(int hours) {
		return IntStream.range(0, hours).mapToObj(h -> row(T0.plusMillis(HOUR * h), h % 2 == 0 ? "a" : "b", h));
	}

	static SeriesAggregator agg() {
		return SeriesAggregator.create().timeField("time").allowFields("org", "ms");
	}

	static Map<String,List<String>> valueless(String name, String...kv) {
		var m = q(kv);
		m.put(name, Collections.singletonList(null));
		return m;
	}

	static String iso(long ms) { return Instant.ofEpochMilli(ms).toString(); }

	@Test void a01_dayCountAndSum() {
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + 2 * DAY),
			"bucket", "day", "measure", "count", "measure", "sum(ms)")), hourly(48));
		assertEquals(List.of("bucket", "count", "sum(ms)"), r.dims());
		assertEquals(List.of(T0.toEpochMilli(), T0.toEpochMilli() + DAY), r.columns().get("bucket"));
		assertEquals(List.of(24L, 24L), r.columns().get("count"));
		assertEquals(List.of(276.0, 852.0), r.columns().get("sum(ms)"));
		assertFalse(r.truncated());
	}

	@Test void a02_groupByAndAvgMinMax() {
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + DAY),
			"bucket", "day", "groupBy", "org", "measure", "avg(ms)", "measure", "min(ms)", "measure", "max(ms)")),
			hourly(24));
		assertEquals(List.of("bucket", "org", "avg(ms)", "min(ms)", "max(ms)"), r.dims());
		assertEquals(List.of("a", "b"), r.columns().get("org"));
		assertEquals(List.of(11.0, 12.0), r.columns().get("avg(ms)"));
		assertEquals(List.of(0.0, 1.0), r.columns().get("min(ms)"));
		assertEquals(List.of(22.0, 23.0), r.columns().get("max(ms)"));
	}

	@Test void a03_defaultsAndWindow() {
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", "" + T0.toEpochMilli(), "end",
			"" + (T0.toEpochMilli() + 3 * HOUR))), hourly(5));
		assertEquals(List.of("bucket", "count"), r.dims(), "bucket=auto and measure=count by default");
		assertEquals(List.of(T0.toEpochMilli(), T0.toEpochMilli() + HOUR, T0.toEpochMilli() + 2 * HOUR),
			r.columns().get("bucket"),
			"auto over 3 hours is hour; hours 3 and 4 are outside [start, end)");
	}

	@Test void a04_autoUnitTargetsAThousandBuckets() {
		var a = agg();
		assertEquals("hour", a.autoUnit(T0, T0.plusMillis(1000 * HOUR)));
		assertEquals("day", a.autoUnit(T0, T0.plusMillis(1001 * HOUR)));
		assertEquals("week", a.autoUnit(T0, T0.plusMillis(1001 * DAY)));
		assertEquals("month", a.autoUnit(T0, T0.plusMillis(30_000 * DAY)), "month when no unit fits");
	}

	@Test void a05_weekStartsAndMonthEdges() {
		var day = SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + DAY), "bucket",
			"week"));
		assertEquals(List.of(T0.toEpochMilli()), agg().aggregate(day, hourly(1)).columns().get("bucket"),
			"Monday by default");
		assertEquals(List.of(T0.toEpochMilli() - DAY), agg().weekStart(DayOfWeek.SUNDAY).aggregate(day, hourly(1))
			.columns().get("bucket"));
		var jan31 = Instant.parse("2026-01-31T23:00:00Z");
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", "2026-01-01T00:00:00Z", "end", "2026-03-01T00:00:00Z",
			"bucket", "month")),
			Stream.of(row(jan31, "a", 1), row(jan31.plusMillis(HOUR), "a", 1)));
		assertEquals(List.of(Instant.parse("2026-01-01T00:00:00Z").toEpochMilli(), Instant.parse("2026-02-01T00:00:00Z")
			.toEpochMilli()), r.columns().get("bucket"));
	}

	@Test void a06_appFiltersPassThrough() {
		var req = SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + DAY), "bucket",
			"day", "search", "x", "search", "y", "measure", "count"));
		assertEquals(Map.of("search", List.of("x", "y")), req.filters());
		assertEquals(T0, req.start());
		var r = agg().aggregate(req, hourly(24));
		assertEquals(List.of(24L), r.columns().get("count"), "the aggregator does not apply app filters");
	}

	@Test void a07_cellCapStopsNewCellsOnly() {
		var pulled = new AtomicInteger();
		var r = agg().maxBuckets(2)
			.aggregate(SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + DAY),
			"bucket", "hour", "groupBy", "org")), hourly(6).peek(x -> pulled.incrementAndGet()));
		assertEquals(List.of(T0.toEpochMilli(), T0.toEpochMilli() + HOUR), r.columns().get("bucket"));
		assertEquals(List.of(1L, 1L), r.columns().get("count"));
		assertTrue(r.truncated());
		assertEquals(6, pulled.get(), "one pass, read to the end");
	}

	@Test void a08_response() {
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + HOUR),
			"bucket", "hour")), hourly(1));
		assertEquals("{\"schemaVersion\":1,\"dims\":[{\"name\":\"bucket\",\"type\":\"time\"},"
			+ "{\"name\":\"count\",\"type\":\"number\"}],"
			+ "\"columns\":{\"bucket\":[" + T0.toEpochMilli() + "],\"count\":[1]},\"truncated\":false}", r.toMap()
			.toString());
	}

	@Test void a09_rejected() {
		var w = new String[] { "start", T0.toString(), "end", iso(T0.toEpochMilli() + 1) };
		var cases = new LinkedHashMap<Map<String,List<String>>,String>();
		cases.put(q("end", T0.toString()), "'start' is required");
		cases.put(q("start", T0.toString(), "end", T0.toString()), "'end' must be after 'start'");
		cases.put(q("start", "nope", "end", T0.toString()),
			"'start' is not an ISO-8601 instant or epoch milliseconds: 'nope'");
		cases.put(q(w[0], w[1], w[2], w[3], "bucket", "year"), "unknown bucket 'year'");
		cases.put(q(w[0], w[1], w[2], w[3], "tz", "America/New_York"),
			"only tz 'UTC' is supported, not 'America/New_York'");
		cases.put(q(w[0], w[1], w[2], w[3], "groupBy", "a,b,c,d"), "at most 3 groupBy fields, not 4");
		cases.put(q(w[0], w[1], w[2], w[3], "measure", "median(ms)"), "unknown measure 'median(ms)'");
		cases.put(q(w[0], w[1], w[2], w[3], "measure", "sum:ms"), "unknown measure 'sum:ms'");
		cases.put(q("start", "99999999999999999999", "end", T0.toString()),
			"'start' is not an ISO-8601 instant or epoch milliseconds: '99999999999999999999'");
		cases.put(q(w[0], w[1], w[2], w[3], "groupBy", "org,org"), "duplicate groupBy field 'org'");
		cases.put(q(w[0], w[1], w[2], w[3], "groupBy", "bucket"), "groupBy field 'bucket' is reserved");
		cases.put(q(w[0], w[1], w[2], w[3], "groupBy", "count"), "groupBy field 'count' collides with a measure");
		cases.put(q(w[0], w[1], w[2], w[3], "measure", "count", "measure", "count"), "duplicate measure 'count'");
		cases.put(valueless("start"), "'start' is required");
		cases.forEach((query, reason) -> {
			var e = assertThrows(SeriesAggregator.RequestException.class, () -> SeriesAggregator.Request.of(query));
			assertEquals("E-75: aggregate request rejected: " + reason, e.getMessage());
			assertEquals(400, e.getStatusCode());
		});
		var e = assertThrows(SeriesAggregator.RequestException.class, () -> SeriesAggregator.create().aggregate(
			SeriesAggregator.Request.of(q(w[0], w[1], w[2], w[3], "groupBy", "org")), Stream.empty()));
		assertEquals("E-75: aggregate request rejected: field 'org' is not allowed", e.getMessage(),
			"no allowFields rejects every field");
	}

	@Test void a10_valuelessFilterIsEmptyString() {
		var req = SeriesAggregator.Request.of(valueless("debug", "start", T0.toString(), "end",
			iso(T0.toEpochMilli() + DAY)));
		assertEquals(Map.of("debug", List.of("")), req.filters());
	}

	@Test void a11_dateSubclassWhoseToInstantThrows() {
		var t = new Date(T0.toEpochMilli() + HOUR) {
			private static final long serialVersionUID = 1L;
			@Override public Instant toInstant() { throw new UnsupportedOperationException(); }
		};
		var m = new HashMap<String,Object>();
		m.put("time", t);
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + DAY),
			"bucket", "day")),
			Stream.of(SeriesSource.Row.of(m)));
		assertEquals(List.of(1L), r.columns().get("count"));
	}

	@Test void a12_localDateTimeIsUtc() {
		var m = new HashMap<String,Object>();
		m.put("time", LocalDateTime.of(2026, 10, 5, 3, 0));
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + DAY),
			"bucket", "day")),
			Stream.of(SeriesSource.Row.of(m)));
		assertEquals(List.of(1L), r.columns().get("count"));
	}

	@Test void a13_overflowingSumIsNull() {
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + DAY),
			"bucket", "day", "measure", "sum(ms)")),
			Stream.of(row(T0, "a", Double.MAX_VALUE), row(T0, "a", Double.MAX_VALUE)));
		assertEquals(Collections.singletonList(null), r.columns().get("sum(ms)"));
	}

	@Test void a14_nullGroupIsItsOwnCell() {
		var r = agg()
			.aggregate(SeriesAggregator.Request.of(q("start", T0.toString(), "end", iso(T0.toEpochMilli() + DAY),
			"bucket", "day", "groupBy", "org")),
			Stream.of(row(T0, "null", 1), row(T0, null, 2), row(T0, "a", 3), row(T0, null, 4)));
		assertEquals(Arrays.asList(null, "a", "null"), r.columns().get("org"));
		assertEquals(List.of(2L, 1L, 1L), r.columns().get("count"));
		assertEquals(Arrays.asList(null, "a", "null"), r.toMap().getMap("columns").get("org"));
	}
}
