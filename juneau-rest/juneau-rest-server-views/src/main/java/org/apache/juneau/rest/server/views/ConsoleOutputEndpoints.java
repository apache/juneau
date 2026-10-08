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
package org.apache.juneau.rest.server.views;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.response.BadRequest;
import org.apache.juneau.http.response.Gone;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.rest.server.*;

/**
 * Static helpers that implement the console-output lines and download endpoints.
 *
 * <p>
 * {@link ConsoleOutputMixin} delegates to these; an application that wants its own paths writes two
 * {@code @RestGet} methods that call {@link #lines(Optional, RestRequest, RestResponse)} and
 * {@link #download(Optional, String, RestResponse)} and gets the same validation, caps and headers.
 *
 * @since 10.0.0
 */
public final class ConsoleOutputEndpoints {

	/** Forward-page line cap. */
	public static final int MAX_PAGE_LINES = 2000;

	/** Largest {@code tail} or {@code limit} value. */
	public static final int MAX_LIMIT = 10_000;

	/** {@code limit} used by a {@code ?before=} request that does not send one. */
	public static final int DEFAULT_LIMIT = 2000;

	/** The download writer flushes after this many lines. */
	static final int FLUSH_EVERY = 1000;

	private ConsoleOutputEndpoints() {}

	/**
	 * Checks a mixin {@code logId} against {@code ^[A-Za-z0-9_-]{1,128}$}.
	 *
	 * @param logId The log id from the URL path.
	 * @return The same id.
	 * @throws BadRequest If the id does not match.
	 */
	public static String checkLogId(String logId) {
		if (! ConsoleOutputChecks.isLogId(logId))
			throw new BadRequest("logId must match [A-Za-z0-9_-]{1,128}");
		return logId;
	}

	/**
	 * Serves one page of the lines endpoint ({@code ?after=}, {@code ?tail=}, {@code ?before=&limit=}) as JSON.
	 *
	 * @param source The resolved source; empty answers {@code 404}.
	 * @param req The request.
	 * @param res The response.
	 * @throws IOException If the response could not be written.
	 */
	public static void lines(Optional<ConsoleOutputSource> source, RestRequest req, RestResponse res) throws IOException {
		var src = source.orElseThrow(NotFound::new);
		var after = query(req, "after");
		var tail = query(req, "tail");
		var before = query(req, "before");
		var limit = query(req, "limit");
		var forms = (after != null ? 1 : 0) + (tail != null ? 1 : 0) + (before != null ? 1 : 0);
		if (forms > 1)
			throw new BadRequest("at most one of after, tail and before may be given");
		if (limit != null && before == null)
			throw new BadRequest("limit requires before");
		ConsoleOutputPage page;
		try {
			if (tail != null)
				page = src.tail(intParam("tail", tail));
			else if (before != null)
				page = src.before(token("before", before), limit == null ? DEFAULT_LIMIT : intParam("limit", limit));
			else
				page = src.page(after == null ? null : token("after", after), MAX_PAGE_LINES);
		} catch (ConsoleOutputSource.UnknownTokenException e) {
			throw new Gone("unknown or stale console-output token");
		}
		page.validate();
		res.setHeader("Cache-Control", "no-store");
		try (var w = res.getDirectWriter("application/json")) {
			JsonSerializer.DEFAULT.write(page.toContractMap(), w);
		}
	}

	/**
	 * Streams the whole log as JSONL, one compact contract map per line, as an attachment named
	 * {@code <logId>.jsonl}.
	 *
	 * @param source The resolved source; empty answers {@code 404}.
	 * @param logId The log id (also the download file name).
	 * @param res The response.
	 * @throws IOException If the response could not be written.
	 */
	public static void download(Optional<ConsoleOutputSource> source, String logId, RestResponse res) throws IOException {
		checkLogId(logId);
		var src = source.orElseThrow(NotFound::new);
		res.setHeader("Cache-Control", "no-store");
		res.setHeader("Content-Disposition", "attachment; filename=\"" + fileName(logId) + ".jsonl\"");
		try (var lines = src.stream(); var w = res.getDirectWriter("application/jsonl")) {
			var count = 0;
			for (var it = lines.iterator(); it.hasNext();) {
				JsonlSerializer.DEFAULT.write(it.next().toContractMap(), w);
				if (++count % FLUSH_EVERY == 0)
					w.flush();
			}
			w.flush();
		}
	}

	/** Replaces every character outside {@code [A-Za-z0-9._-]} with {@code _}. */
	static String fileName(String s) {
		return s.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private static String query(RestRequest req, String name) {
		return req.getQueryParam(name).asString().orElse(null);
	}

	private static String token(String name, String value) {
		if (! ConsoleOutputChecks.isToken(value))
			throw new BadRequest("%s is not a valid token", name);
		return value;
	}

	private static int intParam(String name, String value) {
		int i;
		try {
			i = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			i = -1;
		}
		if (i < 1 || i > MAX_LIMIT)
			throw new BadRequest("%s must be an integer in 1..%s", name, MAX_LIMIT);
		return i;
	}
}
