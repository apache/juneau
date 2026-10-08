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

import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin interface that serves the run-view events endpoint.
 *
 * <p>
 * The one abstract method is the application's resolver, which is where authorization lives: an unknown id and a
 * forbidden id both answer {@code 404}, so ids cannot be enumerated.  Run ids are opaque URL tokens matching
 * {@code ^[A-Za-z0-9_-]{1,128}$}; anything else answers {@code 400} before the resolver runs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> RunsResource <jk>extends</jk> BasicRestServlet <jk>implements</jk> RunViewMixin {
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Optional&lt;RunViewSource&gt; runViewSource(String <jv>runId</jv>, RestRequest <jv>req</jv>) {
 * 			<jk>return</jk> <jf>runs</jf>.find(<jv>runId</jv>).map(Run::events);
 * 		}
 * 	}
 * </p>
 */
public interface RunViewMixin {

	/** The URL path prefix for the run-view endpoint (relative to the host mount). */
	String RUN_VIEW_PREFIX = "/juneau-run-view";

	/** The URL path template of the events endpoint. */
	String EVENTS_PATH = RUN_VIEW_PREFIX + "/{runId}/events";

	/**
	 * Resolves the source for a run id, applying the application's own access rules.
	 *
	 * @param runId The run id from the URL path (already checked against the grammar).
	 * @param req The in-flight request.
	 * @return The source, or empty when the id is unknown or the caller may not read it.
	 */
	Optional<RunViewSource> runViewSource(String runId, RestRequest req);

	/**
	 * [GET /juneau-run-view/{runId}/events] &mdash; one page of events as JSON.
	 *
	 * @param runId The run id.
	 * @param req The request.
	 * @param res The response.
	 * @throws IOException If the response could not be written.
	 */
	@RestGet(path=EVENTS_PATH, summary="Run view events", serializers=JsonSerializer.class, swagger=@OpSwagger(ignore=true))
	default void runViewEvents(@Path("runId") String runId, RestRequest req, RestResponse res) throws IOException {
		RunViewEndpoints.events(runViewSource(RunViewEndpoints.checkRunId(runId), req), req, res);
	}
}
