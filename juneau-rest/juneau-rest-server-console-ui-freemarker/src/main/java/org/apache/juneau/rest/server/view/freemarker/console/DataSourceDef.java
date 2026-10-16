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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.commons.http.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;

/**
 * Declares a console data source: a browser loader that fetches rows from a same-origin URL, builds a columnar frame
 * and publishes it on the topic <c>data:&lt;id&gt;</c>, which chart cards read with {@code ChartDef.source(...)}.
 *
 * <p>
 * Add it to a page with {@code PageSpec.data(...)}.  The declaration is checked when it is added: a bad URL, mode,
 * chunking, column type or subscription role is E-76, and a bad parser or builder name is E-74.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>page</jv>.data(DataSourceDef.<jsm>create</jsm>(<js>"orgRequests"</js>)
 * 		.url(<js>"/rest/orgRequests/series"</js>).format(Format.<jsf>CSV</jsf>)
 * 		.params(Subscription.<jsm>to</jsm>(Topics.<jsm>filter</jsm>(<js>"range"</js>)).as(<js>"params"</js>)
 * 			.map(<js>"start"</js>, <js>"start"</js>).map(<js>"end"</js>, <js>"end"</js>))
 * 		.chunk(Chunking.<jsm>byDay</jsm>(<js>"start"</js>, <js>"end"</js>)).cache(Cache.<jsm>indexedDb</jsm>()));
 * </p>
 *
 * @since 10.0.0
 */
public final class DataSourceDef {

	static final String E76 = "E-76: data source '%s': %s";
	static final Set<String> COLUMN_TYPES = Set.of("number", "time", "string", "bool");
	static final Set<String> BUCKETS = Set.of("hour", "day", "week", "month", "auto");
	static final Pattern FIELD = Pattern.compile("^[^,()\\s]+$");
	static final Pattern MEASURE = Pattern.compile("^(count|(sum|avg|min|max)\\([^,()\\s]+\\))$");
	static final Pattern DOTTED_PATH = Pattern.compile("^[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*$");

	/** The built-in parser for each wire format. */
	public enum Format {
		/** RFC 4180 CSV with a header row; built-in parser <c>csv</c>. */
		CSV,
		/** One JSON object per line; built-in parser <c>ndjson</c>. */
		NDJSON,
		/** An array of rows or <c>{rows, truncated}</c>; built-in parser <c>json</c>. */
		JSON
	}

	/** How the frame is produced. */
	public enum Mode {
		/** The rows as streamed. */
		RAW,
		/** One request to the aggregate URL. */
		AGGREGATE,
		/** RAW, switching to AGGREGATE past {@link DataSourceDef#maxRows(long)} or on a truncated chunk. */
		AUTO
	}

	/** How the window is split into requests; UTC calendar units. */
	public static final class Chunking {
		final String unit, start, end;

		private Chunking(String unit, String start, String end) {
			this.unit = unit;
			this.start = start;
			this.end = end;
		}

		/**
		 * One request per UTC hour.
		 *
		 * @param start The query parameter (and params name) carrying the window start.
		 * @param end The query parameter (and params name) carrying the window end.
		 * @return A new chunking.
		 */
		public static Chunking byHour(String start, String end) { return new Chunking("hour", start, end); }

		/**
		 * One request per UTC day.
		 *
		 * @param start The window-start parameter name.
		 * @param end The window-end parameter name.
		 * @return A new chunking.
		 */
		public static Chunking byDay(String start, String end) { return new Chunking("day", start, end); }

		/**
		 * One request per ISO week (Monday start, UTC).
		 *
		 * @param start The window-start parameter name.
		 * @param end The window-end parameter name.
		 * @return A new chunking.
		 */
		public static Chunking byWeek(String start, String end) { return new Chunking("week", start, end); }

		/** @return No chunking: one request. */
		public static Chunking none() { return new Chunking(null, null, null); }
	}

	/** Where closed chunks are kept. */
	public static final class Cache {
		final String kind;

		private Cache(String kind) { this.kind = kind; }

		/** @return IndexedDB, falling back to memory (E-JS-101) when unavailable. */
		public static Cache indexedDb() { return new Cache("indexedDb"); }

		/** @return Memory for the page's life (the default). */
		public static Cache memory() { return new Cache("memory"); }

		/** @return No cache. */
		public static Cache none() { return new Cache("none"); }
	}

	/** The aggregate request: a bucket unit, up to 3 groupBy dims, and the measures. */
	public static final class Aggregate {
		final String bucket;
		final List<String> groupBy = new ArrayList<>();
		final List<String> measures = new ArrayList<>();

		private Aggregate(String bucket) { this.bucket = bucket; }

		/**
		 * @param unit <c>hour</c>, <c>day</c>, <c>week</c>, <c>month</c> or <c>auto</c>.
		 * @return A new aggregate spec.
		 */
		public static Aggregate bucket(String unit) { return new Aggregate(unit); }

		/** @param fields Up to 3 dims. @return This object. */
		public Aggregate groupBy(String...fields) { Collections.addAll(groupBy, fields); return this; }

		/**
		 * @param values <c>count</c>, <c>sum(f)</c>, <c>avg(f)</c>, <c>min(f)</c> or <c>max(f)</c>.
		 * @return This object.
		 */
		public Aggregate measure(String...values) { Collections.addAll(measures, values); return this; }
	}

	private final String id;
	private String url, parser = "csv", builder = "columnar", aggregateUrl, epochUrl, epochPath;
	private final List<JsonMap> columns = new ArrayList<>();
	private final List<JsonMap> subscribes = new ArrayList<>();
	private Chunking chunk = Chunking.none();
	private int concurrency = 1, debounceMs = 250;
	private Cache cache = Cache.memory();
	private Mode mode = Mode.RAW;
	private Long maxRows;
	private Aggregate aggregate;

	private DataSourceDef(String id) {
		Topics.data(id);
		this.id = checkedId(id);
	}

	static String checkedId(String id) {
		if (! FtlAttrLists.ID_PATTERN.matcher(id).matches())
			throw iaex(E76, id, "id must match " + FtlAttrLists.ID_PATTERN.pattern());
		return id;
	}

	/**
	 * @param id The source id; unique across cards, data sources and dashboard sections.  The topic is
	 * 	<c>data:&lt;id&gt;</c>.
	 * @return A new declaration.
	 * @throws IllegalArgumentException E-40 if the id is not a valid topic key.
	 */
	public static DataSourceDef create(String id) {
		return new DataSourceDef(id);
	}

	/** @return The source id. */
	public String id() { return id; }

	private IllegalArgumentException e76(String reason, Object...args) {
		return iaex(E76, id, f(reason, args));
	}

	private void required(Object value, String what) {
		if (value == null)
			throw e76("%s is null", what);
	}

	private String sameOrigin(String what, String value) {
		if (! SafePathTemplate.isSameOriginPath(value))
			throw e76("%s '%s' is not a same-origin path", what, value);
		if (value.indexOf('#') >= 0)
			throw e76("%s '%s' has a fragment", what, value);
		return value;
	}

	/** @param value A same-origin path; <c>{token}</c>s are filled from params. @return This object. */
	public DataSourceDef url(String value) { url = sameOrigin("url", value); return this; }

	/** @param value The wire format; sets the built-in parser. @return This object. */
	public DataSourceDef format(Format value) {
		required(value, "format");
		parser = value.name().toLowerCase(Locale.ROOT);
		return this;
	}

	/**
	 * @param name A registered parser: a bare built-in name or a dotted app name.  Only the name's shape is checked
	 * 	here; a bare name other than the built-ins (<c>csv</c>, <c>ndjson</c>, <c>json</c>) is resolved only in the
	 * 	browser, where an unknown one is E-JS-92.
	 * @return This object.
	 */
	public DataSourceDef parser(String name) { parser = extensionName(name, "parser"); return this; }

	/**
	 * @param name A registered builder: a bare built-in name or a dotted app name.  Only the name's shape is checked
	 * 	here; a bare name other than the built-in <c>columnar</c> is resolved only in the browser, where
	 * 	an unknown one is E-JS-93.
	 * @return This object.
	 */
	public DataSourceDef builder(String name) { builder = extensionName(name, "builder"); return this; }

	static String extensionName(String name, String what) {
		if (name == null || ! BusWiringValidator.EXTENSION_NAME.matcher(name).matches())
			throw new IllegalArgumentException("E-74: " + f(BusWiringValidator.E74, name, what));
		return name;
	}

	/** @param specs Columns as <c>name:type</c>, type one of number, time, string, bool. @return This object. */
	public DataSourceDef columns(String...specs) {
		for (var s : specs) {
			if (s == null)
				throw e76("column is null");
			var i = s.indexOf(':');
			if (i < 1 || s.substring(0, i).isBlank())
				throw e76("column '%s' needs the form name:type", s);
			var type = s.substring(i + 1);
			if (! COLUMN_TYPES.contains(type))
				throw e76("column '%s' type '%s' is not number, time, string or bool", s, type);
			var name = s.substring(0, i);
			if (columns.stream().anyMatch(c -> name.equals(c.get("name"))))
				throw e76("column '%s' is declared twice", name);
			columns.add(JsonMap.of("name", name, "type", type));
		}
		return this;
	}

	/** @param subs Subscriptions with role <c>params</c> or <c>refresh</c>. @return This object. */
	public DataSourceDef params(Subscription...subs) {
		for (var s : subs) {
			required(s, "subscription");
			var m = s.toMap();
			var role = m.get("as");
			if (! "params".equals(role) && ! "refresh".equals(role))
				throw e76("subscription to '%s' has role '%s'; a data source accepts params or refresh",
					s.topic(), role);
			subscribes.add(m);
		}
		return this;
	}

	/**
	 * @param value The chunking; {@link Chunking#none()} by default.
	 * @return This object.
	 * @throws IllegalArgumentException E-76 if the value is null, or the window param names are blank or equal.
	 */
	public DataSourceDef chunk(Chunking value) {
		required(value, "chunk");
		if (value.unit != null && (value.start == null || value.start.isBlank() || value.end == null
			|| value.end.isBlank() || value.start.equals(value.end)))
			throw e76("chunk window params '%s' and '%s' must be non-blank and different", value.start, value.end);
		chunk = value;
		return this;
	}

	/** @param value Parallel chunk requests, 1..6 (default 1). @return This object. */
	public DataSourceDef concurrency(int value) {
		if (value < 1 || value > 6)
			throw e76("concurrency %s is outside 1..6", value);
		concurrency = value;
		return this;
	}

	/** @param value The params-change debounce in ms (default 250). @return This object. */
	public DataSourceDef debounceMs(int value) {
		if (value < 0)
			throw e76("debounceMs %s is negative", value);
		debounceMs = value;
		return this;
	}

	/** @param value The cache; {@link Cache#memory()} by default. @return This object. */
	public DataSourceDef cache(Cache value) { required(value, "cache"); cache = value; return this; }

	/**
	 * @param epochUrl A same-origin path probed once per load.
	 * @param path The dotted path of the epoch value in the JSON response.
	 * @return This object.
	 */
	public DataSourceDef epoch(String epochUrl, String path) {
		var checkedUrl = sameOrigin("epoch url", epochUrl);
		if (path == null || ! DOTTED_PATH.matcher(path).matches())
			throw e76("epoch path '%s' is not a dotted path", path);
		this.epochUrl = checkedUrl;
		this.epochPath = path;
		return this;
	}

	/** @param value The mode; {@link Mode#RAW} by default. @return This object. */
	public DataSourceDef mode(Mode value) { required(value, "mode"); mode = value; return this; }

	/** @param value The row count past which AUTO switches to AGGREGATE. @return This object. */
	public DataSourceDef maxRows(long value) {
		if (value <= 0)
			throw e76("maxRows %s is not greater than 0", value);
		maxRows = value;
		return this;
	}

	/** @param value A same-origin path answering the aggregate request. @return This object. */
	public DataSourceDef aggregateUrl(String value) { aggregateUrl = sameOrigin("aggregateUrl", value); return this; }

	/** @param value The aggregate request. @return This object. */
	public DataSourceDef aggregate(Aggregate value) {
		required(value, "aggregate");
		if (value.bucket == null || ! BUCKETS.contains(value.bucket))
			throw e76("aggregate bucket '%s' is not hour, day, week, month or auto", value.bucket);
		for (var g : value.groupBy)
			if (g == null || ! FIELD.matcher(g).matches())
				throw e76("aggregate groupBy field '%s' is not a field name", g);
		for (var m : value.measures)
			if (m == null || ! MEASURE.matcher(m).matches())
				throw e76("aggregate measure '%s' is not count, sum(f), avg(f), min(f) or max(f)", m);
		if (value.groupBy.size() > 3)
			throw e76("aggregate has %s groupBy fields; at most 3", value.groupBy.size());
		var copy = Aggregate.bucket(value.bucket);
		copy.groupBy.addAll(value.groupBy);
		copy.measures.addAll(value.measures);
		aggregate = copy;
		return this;
	}

	/**
	 * The <c>contract.dataSources[]</c> entry; checks the cross-field rules.
	 *
	 * @return A new map.
	 * @throws IllegalArgumentException E-76 if the declaration is incomplete or inconsistent.
	 */
	public JsonMap toMap() {
		if (url == null)
			throw e76("url is required");
		var modeName = mode.name().toLowerCase(Locale.ROOT);
		if (mode != Mode.RAW && (aggregateUrl == null || aggregate == null))
			throw e76("mode %s needs aggregateUrl(...) and aggregate(...)", modeName);
		if (mode == Mode.AUTO && maxRows == null)
			throw e76("mode auto needs maxRows(n) greater than 0");
		if (chunk.unit != null) {
			var mapped = new HashSet<String>();
			for (var s : subscribes)
				if ("params".equals(s.get("as")) && s.get("map") instanceof Map<?,?> mm)
					mm.keySet().forEach(k -> mapped.add(String.valueOf(k)));
			for (var w : List.of(chunk.start, chunk.end))
				if (! mapped.contains(w))
					throw e76("chunk window param '%s' is not mapped by any params subscription", w);
		}
		var m = JsonMap.of("id", id, "kind", "source", "url", url, "parser", parser, "builder", builder);
		if (! columns.isEmpty())
			m.put("columns", columns.stream().map(JsonMap::new).toList());
		m.put("subscribes", subscribes.stream().map(JsonMap::new).toList());
		if (chunk.unit != null)
			m.put("chunk", JsonMap.of("unit", chunk.unit, "start", chunk.start, "end", chunk.end));
		m.put("concurrency", concurrency);
		m.put("debounceMs", debounceMs);
		m.put("cache", cache.kind);
		if (epochUrl != null)
			m.put("epoch", JsonMap.of("url", epochUrl, "path", epochPath));
		m.put("mode", modeName);
		if (maxRows != null)
			m.put("maxRows", maxRows);
		if (aggregateUrl != null)
			m.put("aggregateUrl", aggregateUrl);
		if (aggregate != null)
			m.put("aggregate", JsonMap.of("bucket", aggregate.bucket, "groupBy", List.copyOf(aggregate.groupBy),
				"measures", List.copyOf(aggregate.measures.isEmpty() ? List.of("count") : aggregate.measures)));
		return m;
	}
}
