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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.rest.server.*;

/**
 * Streams rows to a console data source as CSV or NDJSON.
 *
 * <p>
 * Rows are written in order and flushed every {@value #FLUSH_ROWS} rows or {@value #FLUSH_CHARS} characters, so the
 * browser can parse while the response is still arriving.  With {@link #cap(long)} the stream stops after that many
 * rows and, when more rows exist, writes the truncation sentinel the browser reports as E-JS-102.  Times are written
 * as ISO-8601 UTC; a {@code LocalDate} is the start of that day in UTC, and a {@code LocalDateTime} is read as UTC.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(path=<js>"/series"</js>)
 * 	<jk>public void</jk> series(RestResponse <jv>res</jv>) <jk>throws</jk> IOException {
 * 		<jk>try</jk> (<jk>var</jk> <jv>rows</jv> = <jv>svc</jv>.rows()) {
 * 			SeriesSource.<jsm>csv</jsm>(<jv>res</jv>).columns(<js>"start"</js>, <js>"v"</js>).cap(100_000)
 * 				.write(<jv>rows</jv>);
 * 		}
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // The output stream belongs to the response and is closed by the container, not by this streamer.
})
public final class SeriesSource {

	/** One row; {@link #get(String)} returns the value for a column, or null. */
	@FunctionalInterface
	public interface Row {

		/**
		 * Returns the value of one column.
		 *
		 * @param column The column name.
		 * @return The value, or <jk>null</jk>.
		 */
		Object get(String column);

		/**
		 * A row backed by a map.
		 *
		 * @param m The map.  Not copied.
		 * @return A new row.
		 */
		static Row of(Map<String,?> m) {
			return m::get;
		}
	}

	/** The output format. */
	public enum Format {
		/** RFC 4180 CSV with a header row and LF line endings. */
		CSV,
		/** One JSON object per line. */
		NDJSON
	}

	/** Rows written between flushes. */
	public static final int FLUSH_ROWS = 1000;

	/** Characters written between flushes. */
	public static final int FLUSH_CHARS = 64 * 1024;

	private final Format format;
	private final OutputStream out;
	private List<String> columns;
	private long cap = Long.MAX_VALUE;

	SeriesSource(Format format, OutputStream out) {
		this.format = format;
		this.out = out;
	}

	/**
	 * A CSV streamer on the response: sets <c>text/csv;charset=utf-8</c> and <c>X-Content-Type-Options: nosniff</c>.
	 *
	 * @param res The response.
	 * @return A new streamer.
	 * @throws IOException If the output stream cannot be opened.
	 */
	public static SeriesSource csv(RestResponse res) throws IOException {
		return open(res, Format.CSV, "text/csv;charset=utf-8");
	}

	/**
	 * An NDJSON streamer on the response: sets <c>application/x-ndjson;charset=utf-8</c> and <c>nosniff</c>.
	 *
	 * @param res The response.
	 * @return A new streamer.
	 * @throws IOException If the output stream cannot be opened.
	 */
	public static SeriesSource ndjson(RestResponse res) throws IOException {
		return open(res, Format.NDJSON, "application/x-ndjson;charset=utf-8");
	}

	private static SeriesSource open(RestResponse res, Format format, String contentType) throws IOException {
		res.setContentType(contentType);
		res.setHeader("X-Content-Type-Options", "nosniff");
		return new SeriesSource(format, res.getOutputStream());
	}

	/**
	 * The columns to write, in order.  CSV writes them as the header row.
	 *
	 * @param names The column names.  At least one.
	 * @return This object.
	 */
	public SeriesSource columns(String...names) {
		if (names.length == 0)
			throw iaex("SeriesSource.columns: at least one column is required");
		columns = List.of(names);
		return this;
	}

	/**
	 * The most rows to write.  When more rows exist, the truncation sentinel follows them.
	 *
	 * @param value The cap.  At least 1.
	 * @return This object.
	 */
	public SeriesSource cap(long value) {
		if (value < 1)
			throw iaex("SeriesSource.cap: '%s' is not a positive row count", value);
		cap = value;
		return this;
	}

	/**
	 * Writes the rows.  Reads at most one row past the cap.  The caller closes the stream.
	 *
	 * @param rows The rows.
	 */
	@SuppressWarnings({
		"java:S2095" // The writer wraps the response stream, which the container closes.
	})
	public void write(Stream<? extends Row> rows) {
		if (columns == null)
			throw isex("SeriesSource.write: columns(...) not set");
		var w = new BufferedWriter(new OutputStreamWriter(out, UTF_8), FLUSH_CHARS);
		try {
			var pending = 0L;
			var count = 0L;
			var rowsSinceFlush = 0;
			if (format == Format.CSV)
				pending += line(w, csvHeader());
			var it = rows.iterator();
			while (it.hasNext()) {
				var r = it.next();
				if (count == cap) {
					line(w, format == Format.CSV ? "#truncated," + cap : "{\"$truncated\":" + cap + "}");
					break;
				}
				pending += line(w, format == Format.CSV ? csvRow(r) : jsonRow(r));
				count++;
				if (++rowsSinceFlush >= FLUSH_ROWS || pending >= FLUSH_CHARS) {
					w.flush();
					rowsSinceFlush = 0;
					pending = 0;
				}
			}
			w.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static int line(Writer w, String s) throws IOException {
		w.write(s);
		w.write('\n');
		return s.length() + 1;
	}

	private String csvHeader() {
		return columns.stream().map(SeriesSource::csvField).collect(Collectors.joining(","));
	}

	private String csvRow(Row r) {
		var sb = new StringBuilder();
		for (var i = 0; i < columns.size(); i++) {
			if (i > 0) sb.append(',');
			var v = r.get(columns.get(i));
			if (v != null) sb.append(csvField(text(v)));
		}
		return sb.toString();
	}

	private static String text(Object v) {
		if (Values.isTime(v)) return Values.iso(Values.toInstant(v));
		return String.valueOf(v);
	}

	// Quotes a field holding a comma, quote, CR or LF; one starting with '#' (so data never reads as the
	// sentinel); and the empty string (so it stays distinct from null, which is written as nothing).
	static String csvField(String s) {
		if (s.isEmpty() || s.charAt(0) == '#' || s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0
			|| s.indexOf('\r') >= 0)
			return '"' + s.replace("\"", "\"\"") + '"';
		return s;
	}

	private String jsonRow(Row r) {
		var sb = new StringBuilder("{");
		for (var i = 0; i < columns.size(); i++) {
			if (i > 0) sb.append(',');
			jsonString(sb, columns.get(i)).append(':');
			jsonValue(sb, r.get(columns.get(i)));
		}
		return sb.append('}').toString();
	}

	private static void jsonValue(StringBuilder sb, Object v) {
		if (v == null) sb.append("null");
		else if (Values.isTime(v)) jsonString(sb, Values.iso(Values.toInstant(v)));
		else if (v instanceof Number n) sb.append(Values.nonFinite(n) ? "null" : n.toString());
		else if (v instanceof Boolean b) sb.append(b);
		else jsonString(sb, v.toString());
	}

	private static StringBuilder jsonString(StringBuilder sb, String s) {
		sb.append('"');
		for (var i = 0; i < s.length(); i++) {
			var c = s.charAt(i);
			switch (c) {
				case '"' -> sb.append("\\\"");
				case '\\' -> sb.append("\\\\");
				case '\n' -> sb.append("\\n");
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				default -> {
					if (c < 0x20 || c == 0x2028 || c == 0x2029) sb.append(f("\\u%04x", (int)c));
					else sb.append(c);
				}
			}
		}
		return sb.append('"');
	}
}
