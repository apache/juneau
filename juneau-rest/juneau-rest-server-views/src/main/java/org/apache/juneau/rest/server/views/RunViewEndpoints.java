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
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.runreport.*;

/**
 * Static helpers that implement the run-view events endpoint.
 *
 * <p>
 * {@link RunViewMixin} delegates to these; an application that wants its own path writes one {@code @RestGet}
 * method that calls {@link #events(Optional, RestRequest, RestResponse)} and gets the same validation, cap and
 * headers.
 */
public final class RunViewEndpoints {

	/** The page size cap of the bundled endpoint. */
	public static final int MAX_ENDPOINT_PAGE_EVENTS = 2000;

	private RunViewEndpoints() {}

	/**
	 * Checks a mixin {@code runId} against {@code ^[A-Za-z0-9_-]{1,128}$}.
	 *
	 * @param runId The run id from the URL path.
	 * @return The same id.
	 * @throws BadRequest If the id does not match.
	 */
	public static String checkRunId(String runId) {
		if (! RunViewChecks.isRunId(runId))
			throw new BadRequest("runId must match [A-Za-z0-9_-]{1,128}");
		return runId;
	}

	/**
	 * Serves one page of the events endpoint ({@code ?after=}) as JSON.
	 *
	 * <p>
	 * Answers {@code 404} for an empty source, {@code 400} for a malformed {@code after}, {@code 410 Gone} for an
	 * unknown or stale token, and otherwise {@code 200} with {@code Cache-Control: no-store}.
	 *
	 * @param source The resolved source; empty answers {@code 404}.
	 * @param req The request.
	 * @param res The response.
	 * @throws IOException If the response could not be written.
	 */
	public static void events(Optional<RunViewSource> source, RestRequest req, RestResponse res) throws IOException {
		var src = source.orElseThrow(NotFound::new);
		var after = req.getQueryParam("after").asString().orElse(null);
		if (after != null && ! RunViewChecks.isToken(after))
			throw new BadRequest("after is not a valid token");
		RunViewPage page;
		try {
			page = src.page(after, MAX_ENDPOINT_PAGE_EVENTS);
		} catch (RunViewSource.UnknownTokenException e) {
			throw new Gone("unknown or stale run-view token");
		}
		page.validate();
		res.setHeader("Cache-Control", "no-store");
		try (var w = res.getDirectWriter("application/json")) {
			JsonSerializer.DEFAULT.write(page.toContractMap(), w);
		}
	}
}
