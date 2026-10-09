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
package org.apache.juneau.rest.server.terminal;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.BadRequest;
import org.apache.juneau.http.response.NotFound;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.runreport.*;
import org.apache.juneau.rest.server.util.*;

/**
 * Static helpers that implement the terminal endpoints and serve the terminal runtime.
 *
 * <p>
 * {@link TerminalMixin} delegates to these; an application that wants its own paths writes {@code @RestGet} methods
 * that call them and gets the same validation, cap and headers.
 *
 * <h5 class='section'>Bytes headers:</h5>
 * <ul>
 * 	<li>{@code Term-Next}: the offset to ask for next.
 * 	<li>{@code Term-End}: the source's current end.
 * 	<li>{@code Term-Done}: {@code true} once the producer is finished and the reader is at the end.
 * 	<li>{@code Term-Cols}, {@code Term-Rows}: the producer's terminal size.
 * 	<li>{@code Term-Truncated}: {@code true} when the bytes start later than {@code from}.
 * 	<li>{@code Term-Error}: only ever {@code gone}, when the log disappeared.
 * </ul>
 */
public final class TerminalEndpoints {

	/** Classpath location of the terminal runtime. */
	static final String JS_RESOURCE = "/org/apache/juneau/terminal/juneau-terminal.js";

	/** Classpath location of the terminal stylesheet. */
	static final String CSS_RESOURCE = "/org/apache/juneau/terminal/juneau-terminal.css";

	private static final String JS_CONTENT_TYPE = "text/javascript;charset=utf-8";
	private static final String CSS_CONTENT_TYPE = "text/css;charset=utf-8";
	private static final String CACHE_CONTROL = "max-age=86400, public";
	private static final String OCTETS = "application/octet-stream";

	/** Anchored on this class so the cache-buster carries this module's version. */
	private static final ClasspathAssetCache ASSET_CACHE = new ClasspathAssetCache(TerminalEndpoints.class);

	private TerminalEndpoints() {}

	/**
	 * Checks a terminal id against {@code ^[A-Za-z0-9_-]{1,128}$}.
	 *
	 * @param id The id from the URL path.
	 * @return The same id.
	 * @throws BadRequest If the id does not match.
	 */
	public static String checkId(String id) {
		if (! RunViewChecks.isRunId(id))
			throw new BadRequest("id must match [A-Za-z0-9_-]{1,128}");
		return id;
	}

	/**
	 * Serves {@code ?from=<offset>&max=<n>} as raw bytes with the {@code Term-*} headers.
	 *
	 * <p>
	 * Answers {@code 404} for an empty source, {@code 400} for a malformed {@code from} or {@code max}, {@code 416}
	 * with {@code Term-End} when {@code from} is past the end, and otherwise {@code 200}.  A {@code from} equal to
	 * the end is a {@code 200} with no bytes.
	 *
	 * <p>
	 * A gone source answers {@code 200} with no bytes, {@code Term-Done: true} and {@code Term-Error: gone}, whatever
	 * {@code from} is.  Its {@code Term-Next} and {@code Term-End} are {@code from} itself, so a client that keeps
	 * polling never rewinds; the chunk's own offsets are not consulted.
	 *
	 * @param source The resolved source; empty answers {@code 404}.
	 * @param req The request.
	 * @param res The response.
	 * @throws IOException If the source could not be read or the response written.
	 */
	public static void bytes(Optional<TerminalSource> source, RestRequest req, RestResponse res) throws IOException {
		var src = source.orElseThrow(NotFound::new);
		var from = number(req, "from", 0, RunEvent.MAX_SAFE_INT);
		var max = (int)number(req, "max", TerminalSource.MAX_READ_BYTES, Integer.MAX_VALUE);
		var c = src.read(from, Math.min(max, TerminalSource.MAX_READ_BYTES));
		res.setHeader("Cache-Control", "no-store");
		res.setHeader("X-Content-Type-Options", "nosniff");
		res.setHeader("Term-Cols", String.valueOf(c.cols()));
		res.setHeader("Term-Rows", String.valueOf(c.rows()));
		if (c.gone()) {
			res.setHeader("Term-Next", String.valueOf(from));
			res.setHeader("Term-End", String.valueOf(from));
			res.setHeader("Term-Done", "true");
			res.setHeader("Term-Error", "gone");
			write(res, new byte[0]);
			return;
		}
		res.setHeader("Term-End", String.valueOf(c.end()));
		if (from > c.end()) {
			res.setStatus(416);
			return;
		}
		res.setHeader("Term-Next", String.valueOf(c.next()));
		res.setHeader("Term-Done", String.valueOf(c.done()));
		res.setHeader("Term-Truncated", String.valueOf(c.truncated()));
		write(res, c.bytes());
	}

	/**
	 * Serves every byte still held as an attachment named {@code <id>.log}.
	 *
	 * <p>
	 * A gone source still answers {@code 200} with an empty body, and carries {@code Term-Error: gone} as {@code bytes}
	 * does.
	 *
	 * <p>
	 * The download is not capped and takes no range: it streams the whole log, {@link TerminalSource#MAX_READ_BYTES}
	 * at a time, up to the end at the time of the request.  The id is checked here as well as by
	 * {@link TerminalMixin}, because it becomes the file name.
	 *
	 * @param source The resolved source; empty answers {@code 404}.
	 * @param id The terminal id (also the download file name).
	 * @param res The response.
	 * @throws IOException If the source could not be read or the response written.
	 */
	public static void raw(Optional<TerminalSource> source, String id, RestResponse res) throws IOException {
		checkId(id);
		var src = source.orElseThrow(NotFound::new);
		res.setHeader("Cache-Control", "no-store");
		res.setHeader("X-Content-Type-Options", "nosniff");
		if (src.read(0, 0).gone())
			res.setHeader("Term-Error", "gone");
		res.setHeader("Content-Disposition", "attachment; filename=\"" + id + ".log\"");
		res.setContentType(OCTETS);
		var out = res.getOutputStream();
		src.copyTo(out);
		out.flush();
	}

	/**
	 * The terminal runtime.
	 *
	 * @return {@code juneau-terminal.js} as a JavaScript {@link HttpResource}.
	 */
	public static HttpResource script() {
		return ASSET_CACHE.serve(JS_RESOURCE, JS_CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * The terminal stylesheet.
	 *
	 * @return {@code juneau-terminal.css} as a CSS {@link HttpResource}.
	 */
	public static HttpResource stylesheet() {
		return ASSET_CACHE.serve(CSS_RESOURCE, CSS_CONTENT_TYPE, CACHE_CONTROL);
	}

	/**
	 * Returns the absolute URL of a terminal asset, resolved against the request, with a
	 * {@code ?v=<buildVersion>-<hash8>} cache-buster.
	 *
	 * @param req The current request.
	 * @param path {@link TerminalMixin#JS_PATH} or {@link TerminalMixin#CSS_PATH}.
	 * @return The asset URL.
	 * @throws IllegalArgumentException If this module does not ship the given path.
	 */
	public static String terminalAssetUrl(RestRequest req, String path) {
		var resource = resourceFor(path);
		return req.getUriResolver().resolve("servlet:" + path) + ASSET_CACHE.cacheBuster(resource);
	}

	private static String resourceFor(String path) {
		if (TerminalMixin.JS_PATH.equals(path))
			return JS_RESOURCE;
		if (TerminalMixin.CSS_PATH.equals(path))
			return CSS_RESOURCE;
		throw iaex("TerminalEndpoints does not serve '%s'.", path);
	}

	private static long number(RestRequest req, String name, long dflt, long limit) {
		var s = req.getQueryParam(name).asString().orElse(null);
		if (s == null)
			return dflt;
		if (! s.matches("0|[1-9][0-9]{0,15}"))
			throw new BadRequest(name + " must be a non-negative integer");
		var n = Long.parseLong(s);
		if (n > limit)
			throw new BadRequest(name + " must be at most " + limit);
		return n;
	}

	private static void write(RestResponse res, byte[] bytes) throws IOException {
		res.setContentType(OCTETS);
		res.setContentLength(bytes.length);
		var out = res.getOutputStream();
		out.write(bytes);
		out.flush();
	}
}
