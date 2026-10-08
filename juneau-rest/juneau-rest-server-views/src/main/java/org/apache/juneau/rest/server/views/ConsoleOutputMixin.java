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
 * Mixin interface that serves console-output regions: the paged lines endpoint and the JSONL download.
 *
 * <p>
 * The one abstract method is the application's resolver, which is where authorization lives: an unknown id and a
 * forbidden id both answer {@code 404}, so ids cannot be enumerated. Log ids are opaque URL tokens matching
 * {@code ^[A-Za-z0-9_-]{1,128}$}; anything else answers {@code 400} before the resolver runs.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> JobsResource <jk>extends</jk> BasicRestServlet <jk>implements</jk> ConsoleOutputMixin {
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Optional&lt;ConsoleOutputSource&gt; consoleOutputSource(String <jv>logId</jv>, RestRequest <jv>req</jv>) {
 * 			<jk>return</jk> <jf>jobs</jf>.find(<jv>logId</jv>).map(Job::output);
 * 		}
 * 	}
 * </p>
 *
 * @since 10.0.0
 */
public interface ConsoleOutputMixin {

	/** The URL path prefix for the console-output endpoints (relative to the host mount). */
	String CONSOLE_OUTPUT_PREFIX = "/juneau-console-output";

	/** The URL path template of the lines endpoint. */
	String LINES_PATH = CONSOLE_OUTPUT_PREFIX + "/{logId}/lines";

	/** The URL path template of the JSONL download endpoint. */
	String DOWNLOAD_PATH = CONSOLE_OUTPUT_PREFIX + "/{logId}/download";

	/**
	 * Resolves the source for a log id, applying the application's own access rules.
	 *
	 * @param logId The log id from the URL path (already checked against the grammar).
	 * @param req The in-flight request.
	 * @return The source, or empty when the id is unknown or the caller may not read it.
	 */
	Optional<ConsoleOutputSource> consoleOutputSource(String logId, RestRequest req);

	/**
	 * [GET /juneau-console-output/{logId}/lines] &mdash; one page of lines as JSON.
	 *
	 * @param logId The log id.
	 * @param req The request.
	 * @param res The response.
	 * @throws IOException If the response could not be written.
	 */
	@RestGet(path=LINES_PATH, summary="Console output lines", serializers=JsonSerializer.class, swagger=@OpSwagger(ignore=true))
	default void consoleOutputLines(@Path("logId") String logId, RestRequest req, RestResponse res) throws IOException {
		ConsoleOutputEndpoints.lines(consoleOutputSource(ConsoleOutputEndpoints.checkLogId(logId), req), req, res);
	}

	/**
	 * [GET /juneau-console-output/{logId}/download] &mdash; the whole log as a JSONL attachment.
	 *
	 * @param logId The log id.
	 * @param req The request.
	 * @param res The response.
	 * @throws IOException If the response could not be written.
	 */
	@RestGet(path=DOWNLOAD_PATH, summary="Console output download", swagger=@OpSwagger(ignore=true))
	default void consoleOutputDownload(@Path("logId") String logId, RestRequest req, RestResponse res) throws IOException {
		ConsoleOutputEndpoints.download(consoleOutputSource(ConsoleOutputEndpoints.checkLogId(logId), req), logId, res);
	}
}
