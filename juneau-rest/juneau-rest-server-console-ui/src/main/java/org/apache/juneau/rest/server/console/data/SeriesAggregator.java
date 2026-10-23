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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.*;

/**
 * Answers a console data source's aggregate request in one pass over a row stream.
 *
 * <p>
 * The request names a window, a bucket unit, up to three <c>groupBy</c> fields and the measures to compute.  Any other
 * query parameter is an app filter, returned untouched by {@link Request#filters()} for the app's own row query.
 * Fields used by <c>groupBy</c> or a measure must be allowed with {@link #allowFields(String...)}.  A bad request
 * throws {@link RequestException} (HTTP 400, E-75).  At most {@link #maxBuckets(int)} result cells are kept; rows that
 * would open another cell are dropped and the result is marked truncated.
 *
 * <p>
 * A group value is the text of the row's value, or null (JSON <jk>null</jk>) when the row has none, so a missing
 * value and the text <js>"null"</js> are different groups.  The null group sorts first within its bucket.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(path=<js>"/series/aggregate"</js>)
 * 	<jk>public void</jk> aggregate(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) <jk>throws</jk> Exception {
 * 		<jk>var</jk> <jv>q</jv> = SeriesAggregator.Request.<jsm>from</jsm>(<jv>req</jv>);
 * 		<jk>try</jk> (<jk>var</jk> <jv>rows</jv> =
 * 				<jv>svc</jv>.rows(<jv>q</jv>.start(), <jv>q</jv>.end(), <jv>q</jv>.filters())) {
 * 			SeriesAggregator.<jsm>create</jsm>().timeField(<js>"start"</js>)
 * 				.allowFields(<js>"status"</js>, <js>"ms"</js>)
 * 				.maxBuckets(10_000).aggregate(<jv>q</jv>, <jv>rows</jv>).writeTo(<jv>res</jv>);
 * 		}
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192", // Repeated literals are contract wire keys; named constants would obscure the contract shape.
	"java:S3776" // Validation and rendering chains many independent guards; splitting them adds no clarity.
})
public final class SeriesAggregator {

	/** The bucket units, finest first. */
	static final List<String> UNITS = List.of("hour", "day", "week", "month");

	/** The most buckets <c>bucket=auto</c> aims for. */
	static final int AUTO_BUCKETS = 1000;

	/** Query parameters that are part of the contract and never app filters. */
	static final Set<String> RESERVED = Set.of("start", "end", "bucket", "tz", "groupBy", "measure");

	private static final Pattern MEASURE = Pattern.compile("^(sum|avg|min|max)\\(([^()]+)\\)$");

	private String timeField = "time";
	private DayOfWeek weekStart = DayOfWeek.MONDAY;
	private int maxBuckets = 10_000;
	private final Set<String> allowed = new LinkedHashSet<>();

	private SeriesAggregator() {}

	/**
	 * A new aggregator.  Defaults: time field <c>time</c>, weeks start on Monday, at most 10,000 cells, no fields
	 * allowed.
	 *
	 * @return A new aggregator.
	 */
	public static SeriesAggregator create() {
		return new SeriesAggregator();
	}

	/**
	 * The column holding each row's time.
	 *
	 * @param value The column name.
	 * @return This object.
	 */
	public SeriesAggregator timeField(String value) {
		timeField = value;
		return this;
	}

	/**
	 * The first day of a <c>week</c> bucket.
	 *
	 * @param value The day.
	 * @return This object.
	 */
	public SeriesAggregator weekStart(DayOfWeek value) {
		weekStart = value;
		return this;
	}

	/**
	 * The most result cells (bucket and group combinations).
	 *
	 * @param value The cap.  At least 1.
	 * @return This object.
	 */
	public SeriesAggregator maxBuckets(int value) {
		if (value < 1)
			throw iaex("SeriesAggregator.maxBuckets: '%s' is not a positive count", value);
		maxBuckets = value;
		return this;
	}

	/**
	 * The fields a request may group by or measure.  With none, every such field is rejected.
	 *
	 * @param names The field names.
	 * @return This object.
	 */
	public SeriesAggregator allowFields(String...names) {
		allowed.addAll(List.of(names));
		return this;
	}

	/** A rejected aggregate request: HTTP 400 with an E-75 message. */
	@SuppressWarnings({
		"java:S110" // The deep hierarchy comes from the REST framework's BadRequest exception chain, not from this class.
	})
	public static final class RequestException extends BadRequest {
		private static final long serialVersionUID = 1L;

		/**
		 * Constructor.
		 *
		 * @param reason Why the request was rejected.
		 */
		public RequestException(String reason) {
			super("E-75: aggregate request rejected: %s", reason);
		}
	}

	/** One measure: <c>count</c>, or an op over a field. */
	record Measure(String text, String op, String field) {
		static Measure parse(String s) {
			if ("count".equals(s))
				return new Measure(s, "count", null);
			var m = MEASURE.matcher(s);
			if (! m.matches())
				throw new RequestException("unknown measure '" + s + "'");
			return new Measure(s, m.group(1), m.group(2));
		}
	}

	/** A parsed aggregate request. */
	public static final class Request {
		private final Instant start;
		private final Instant end;
		private final String bucket;
		private final List<String> groupBy;
		private final List<Measure> measures;
		private final Map<String,List<String>> filters;

		private Request(Instant start, Instant end, String bucket, List<String> groupBy, List<Measure> measures,
				Map<String,List<String>> filters) {
			this.start = start;
			this.end = end;
			this.bucket = bucket;
			this.groupBy = groupBy;
			this.measures = measures;
			this.filters = filters;
		}

		/**
		 * Parses the request's query parameters.
		 *
		 * @param req The request.
		 * @return The parsed request.
		 * @throws RequestException If the request breaks the contract.
		 */
		public static Request from(RestRequest req) {
			var m = new LinkedHashMap<String,List<String>>();
			req.getQueryParams().forEach(p -> m.computeIfAbsent(p.getName(), k -> new ArrayList<>()).add(p.getValue()));
			return of(m);
		}

		/**
		 * Parses query parameters given as a map.
		 *
		 * @param query Each parameter name and its values, in order.
		 * @return The parsed request.
		 * @throws RequestException If the request breaks the contract.
		 */
		public static Request of(Map<String,? extends List<String>> query) {
			var start = instant(query, "start");
			var end = instant(query, "end");
			if (! end.isAfter(start))
				throw new RequestException("'end' must be after 'start'");
			var bucket = one(query, "bucket", "auto");
			if (! "auto".equals(bucket) && ! UNITS.contains(bucket))
				throw new RequestException("unknown bucket '" + bucket + "'");
			var tz = one(query, "tz", "UTC");
			if (! "UTC".equals(tz))
				throw new RequestException("only tz 'UTC' is supported, not '" + tz + "'");
			var groupBy = split(query.get("groupBy"));
			if (groupBy.size() > 3)
				throw new RequestException("at most 3 groupBy fields, not " + groupBy.size());
			var ms = split(query.get("measure"));
			var measures = (ms.isEmpty() ? List.of("count") : ms).stream().map(Measure::parse).toList();
			var seen = new HashSet<String>();
			for (var g : groupBy) {
				if ("bucket".equals(g))
					throw new RequestException("groupBy field 'bucket' is reserved");
				if (! seen.add(g))
					throw new RequestException("duplicate groupBy field '" + g + "'");
			}
			var texts = new HashSet<String>();
			for (var m : measures) {
				if (! texts.add(m.text()))
					throw new RequestException("duplicate measure '" + m.text() + "'");
				if (seen.contains(m.text()))
					throw new RequestException("groupBy field '" + m.text() + "' collides with a measure");
			}
			var filters = new TreeMap<String,List<String>>();
			query.forEach((k, v) -> {
				if (! RESERVED.contains(k) && v != null)
					filters.put(k, v.stream().map(x -> x == null ? "" : x).toList());
			});
			return new Request(start, end, bucket, groupBy, measures, Collections.unmodifiableMap(filters));
		}

		/** @return The window start (inclusive). */
		public Instant start() { return start; }

		/** @return The window end (exclusive). */
		public Instant end() { return end; }

		/** @return The bucket unit, or <c>auto</c>. */
		public String bucket() { return bucket; }

		/** @return The groupBy fields, in order. */
		public List<String> groupBy() { return groupBy; }

		/** @return The app filters: every non-contract parameter and its values, untouched. */
		public Map<String,List<String>> filters() { return filters; }

		private static String one(Map<String,? extends List<String>> q, String name, String def) {
			var v = q.get(name);
			return v == null || v.isEmpty() || v.get(0) == null || v.get(0).isEmpty() ? def : v.get(0);
		}

		private static Instant instant(Map<String,? extends List<String>> q, String name) {
			var s = one(q, name, null);
			if (s == null)
				throw new RequestException("'" + name + "' is required");
			var i = Values.parseInstant(s);
			if (i == null)
				throw new RequestException(
					"'" + name + "' is not an ISO-8601 instant or epoch milliseconds: '" + s + "'");
			return i;
		}

		// groupBy is comma-separated or repeated; measure takes the same forms (sum(a,b) is not a valid measure
		// anyway).
		private static List<String> split(List<String> values) {
			if (values == null) return List.of();
			return values.stream().filter(Objects::nonNull).flatMap(v -> Arrays.stream(v.split(",")))
				.map(String::trim).filter(s -> ! s.isEmpty()).toList();
		}
	}

	/** An aggregate result in columnar form: the dims, their types, and one value list per dim. */
	public static final class Result {
		private final List<String> dims;
		private final List<String> types;
		private final Map<String,List<Object>> columns;
		private final boolean truncated;

		Result(List<String> dims, List<String> types, Map<String,List<Object>> columns, boolean truncated) {
			this.dims = dims;
			this.types = types;
			this.columns = columns;
			this.truncated = truncated;
		}

		/** @return The dim names: <c>bucket</c>, the groupBy fields, then the measures. */
		public List<String> dims() { return dims; }

		/**
		 * @return Each dim's values, in row order; <c>bucket</c> is epoch milliseconds UTC and a group value may be
		 * 	null.
		 */
		public Map<String,List<Object>> columns() { return columns; }

		/** @return True when the cell cap dropped rows. */
		public boolean truncated() { return truncated; }

		/**
		 * The response body: <c>{schemaVersion, dims:[{name,type}], columns, truncated}</c>.
		 *
		 * @return A new map.
		 */
		public JsonMap toMap() {
			var d = new ArrayList<Object>();
			for (var i = 0; i < dims.size(); i++)
				d.add(JsonMap.of("name", dims.get(i), "type", types.get(i)));
			var c = new JsonMap();
			c.putAll(columns);
			return JsonMap.of("schemaVersion", 1, "dims", d, "columns", c, "truncated", truncated);
		}

		/**
		 * Writes the body as <c>application/json</c> with <c>nosniff</c>.
		 *
		 * @param res The response.
		 * @throws IOException If the writer fails.
		 */
		@SuppressWarnings({
			"resource" // The response writer is owned and closed by the container.
		})
		public void writeTo(RestResponse res) throws IOException {
			res.setContentType("application/json;charset=utf-8");
			res.setHeader("X-Content-Type-Options", "nosniff");
			res.getWriter().write(Json.of(toMap()));
		}
	}

	/**
	 * Aggregates the rows in one pass.  Rows with a null or out-of-window time are skipped.
	 *
	 * @param req The request.
	 * @param rows The rows, already filtered by the app.
	 * @return The result.
	 * @throws RequestException If the request names a field that is not allowed.
	 */
	@SuppressWarnings({
		"java:S135" // Guard clauses keep the row loop readable
	})
	public Result aggregate(Request req, Stream<? extends SeriesSource.Row> rows) {
		var fields = new ArrayList<String>(req.groupBy);
		req.measures.stream().map(Measure::field).filter(Objects::nonNull).forEach(fields::add);
		for (var f : fields)
			if (! allowed.contains(f))
				throw new RequestException("field '" + f + "' is not allowed");
		var unit = "auto".equals(req.bucket) ? autoUnit(req.start, req.end) : req.bucket;
		var cells = new TreeMap<List<Object>,double[][]>(SeriesAggregator::compareKeys);
		var truncated = false;
		var it = rows.iterator();
		while (it.hasNext()) {
			var r = it.next();
			var t = Values.toInstant(r.get(timeField));
			if (t == null || t.isBefore(req.start) || ! t.isBefore(req.end))
				continue;
			var key = new ArrayList<Object>(req.groupBy.size() + 1);
			key.add(floor(t, unit).toEpochMilli());
			for (var g : req.groupBy)
				key.add(r.get(g) == null ? null : String.valueOf(r.get(g)));
			var acc = cells.get(key);
			if (acc == null) {
				if (cells.size() == maxBuckets) {
					truncated = true;
					continue;
				}
				acc = newAccumulators(req.measures.size());
				cells.put(key, acc);
			}
			accumulate(req.measures, acc, r);
		}
		var dims = new ArrayList<String>(List.of("bucket"));
		var types = new ArrayList<String>(List.of("time"));
		dims.addAll(req.groupBy);
		req.groupBy.forEach(g -> types.add("string"));
		req.measures.forEach(m -> { dims.add(m.text()); types.add("number"); });
		var columns = new LinkedHashMap<String,List<Object>>();
		dims.forEach(d -> columns.put(d, new ArrayList<>(cells.size())));
		cells.forEach((k, acc) -> {
			for (var i = 0; i < k.size(); i++)
				columns.get(dims.get(i)).add(k.get(i));
			for (var i = 0; i < req.measures.size(); i++)
				columns.get(req.measures.get(i).text()).add(result(req.measures.get(i).op(), acc[i]));
		});
		return new Result(List.copyOf(dims), List.copyOf(types), columns, truncated);
	}

	// Each acc[i] holds four slots in order: count, sum, min, max.
	private static double[][] newAccumulators(int n) {
		var a = new double[n][];
		for (var i = 0; i < n; i++)
			a[i] = new double[] { 0, 0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
		return a;
	}

	@SuppressWarnings({
		"java:S135" // Guard clauses keep the accumulation readable
	})
	private static void accumulate(List<Measure> measures, double[][] acc, SeriesSource.Row r) {
		for (var i = 0; i < measures.size(); i++) {
			var m = measures.get(i);
			if (m.field() == null) {
				acc[i][0]++;
				continue;
			}
			if (! (r.get(m.field()) instanceof Number n) || Values.nonFinite(n))
				continue;
			var d = n.doubleValue();
			acc[i][0]++;
			acc[i][1] += d;
			acc[i][2] = Math.min(acc[i][2], d);
			acc[i][3] = Math.max(acc[i][3], d);
		}
	}

	// Overflow gives Infinity, which is not valid JSON; write null instead.
	private static Double finite(double d) {
		return Double.isFinite(d) ? d : null;
	}

	private static Object result(String op, double[] a) {
		return switch (op) {
			case "count" -> (long)a[0];
			case "sum" -> finite(a[1]);
			case "avg" -> a[0] == 0 ? null : finite(a[1] / a[0]);
			case "min" -> a[0] == 0 ? null : a[2];
			default -> a[0] == 0 ? null : a[3];
		};
	}

	// The bucket first, then each group value; a null group (a row with no value) sorts before any text.
	@SuppressWarnings({
		"unchecked" // Non-null group values within a series are mutually Comparable.
	})
	private static int compareKeys(List<Object> a, List<Object> b) {
		for (var i = 0; i < a.size(); i++) {
			var x = a.get(i);
			var y = b.get(i);
			if (x == null || y == null) {
				if (x == y) continue;
				return x == null ? -1 : 1;
			}
			var c = ((Comparable<Object>)x).compareTo(y);
			if (c != 0) return c;
		}
		return 0;
	}

	// The smallest unit giving at most AUTO_BUCKETS buckets over the window; month when none does.  Counting stops at
	// AUTO_BUCKETS + 1, so a long window never walks every hour.
	String autoUnit(Instant start, Instant end) {
		for (var u : UNITS) {
			var n = 0;
			for (var t = floor(start, u); t.isBefore(end) && n <= AUTO_BUCKETS; t = next(t, u))
				n++;
			if (n <= AUTO_BUCKETS) return u;
		}
		return "month";
	}

	Instant floor(Instant t, String unit) {
		var d = t.atZone(ZoneOffset.UTC);
		return switch (unit) {
			case "hour" -> d.truncatedTo(ChronoUnit.HOURS).toInstant();
			case "day" -> d.truncatedTo(ChronoUnit.DAYS).toInstant();
			case "week" -> d.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.previousOrSame(weekStart)).toInstant();
			default -> d.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant();
		};
	}

	static Instant next(Instant t, String unit) {
		var d = t.atZone(ZoneOffset.UTC);
		return switch (unit) {
			case "hour" -> d.plusHours(1).toInstant();
			case "day" -> d.plusDays(1).toInstant();
			case "week" -> d.plusWeeks(1).toInstant();
			default -> d.plusMonths(1).toInstant();
		};
	}
}
