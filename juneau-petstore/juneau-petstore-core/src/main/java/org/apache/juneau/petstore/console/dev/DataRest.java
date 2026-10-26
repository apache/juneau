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
import java.time.temporal.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.console.data.*;
import org.apache.juneau.rest.server.view.*;

/**
 * Data sources: a demo page built with {@link DevPageBuilders#data()} only (it has no template twin).
 *
 * <p>
 * {@code /orders} streams one CSV row per UTC hour in {@code [start, end)} with {@link SeriesSource}.  The browser
 * requests it once per UTC day chunk and caches closed days in IndexedDB.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	GET /console/dev/data
 * 	GET /console/dev/data/orders?start=2026-10-01T00:00:00Z&amp;end=2026-10-02T00:00:00Z
 * </p>
 */
@Rest(path="/data", title="Data sources")
@SuppressWarnings({
	"java:S110", // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
	"resource" // The response writer is owned by the servlet container; Eclipse JDT @Owning warning is by design.
})
public class DataRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** The longest window {@code /orders} serves. */
	static final Duration MAX_WINDOW = Duration.ofDays(31);

	/**
	 * Renders the Data sources page.
	 *
	 * @param req The request.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		return twin(req, DevPageBuilders.data());
	}

	/**
	 * Hourly order counts in the window.
	 *
	 * <p>
	 * Writes one row for each UTC hour whose start falls in {@code [floor(start), end)}, where {@code floor(start)} is
	 * {@code start} rounded down to the hour.  So {@code start=00:30&end=02:30} writes the 00:00, 01:00 and 02:00 rows.
	 * A window with {@code end} before {@code start}, or longer than 31 days, is a 400.
	 *
	 * @param start The window start, ISO-8601.
	 * @param end The window end, ISO-8601, exclusive.
	 * @param res The response.
	 * @throws IOException If the response can't be written.
	 */
	@RestGet(path="/orders")
	public void orders(@Query("start") String start, @Query("end") String end, RestResponse res) throws IOException {
		var from = instant("start", start);
		var to = instant("end", end);
		if (to.isBefore(from))
			throw new BadRequest("Window end '%s' is before start '%s'", end, start);
		if (Duration.between(from, to).compareTo(MAX_WINDOW) > 0)
			throw new BadRequest("Window '%s' to '%s' is longer than %s days", start, end, MAX_WINDOW.toDays());
		var first = from.truncatedTo(ChronoUnit.HOURS);
		SeriesSource.csv(res).columns("t", "placed").write(
			Stream.iterate(first, t -> t.isBefore(to), t -> t.plus(1, ChronoUnit.HOURS))
				.map(t -> SeriesSource.Row.of(Map.of("t", t, "placed", placed(t)))));
	}

	/**
	 * Serves the page-local script.
	 *
	 * @param res The response.
	 * @throws IOException If the bundled script can't be read.
	 */
	@RestGet(path="/data.js")
	public void script(RestResponse res) throws IOException {
		try (var in = DataRest.class.getResourceAsStream("data.js")) {
			if (in == null)
				throw new IllegalStateException(
					"Missing classpath resource 'data.js' beside " + DataRest.class.getName());
			res.setContentType("text/javascript;charset=utf-8");
			res.getWriter().write(new String(in.readAllBytes(), UTF_8));
		}
	}

	static long placed(Instant t) {
		return (t.getEpochSecond() / 3600 * 7) % 10 + 1;
	}

	private static Instant instant(String name, String value) {
		if (value == null)
			throw new BadRequest("Missing query parameter '%s'", name);
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException x) {
			throw new BadRequest("Query parameter '%s' is not an ISO-8601 instant", name);
		}
	}
}
