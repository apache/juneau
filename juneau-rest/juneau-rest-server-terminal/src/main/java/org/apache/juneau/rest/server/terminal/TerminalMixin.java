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

import java.io.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;

/**
 * Mixin interface that serves the terminal bytes and raw endpoints, plus this module's own JS and CSS.
 *
 * <p>
 * The one abstract method is the application's resolver, which is where authorization lives: an unknown id and a
 * forbidden id both answer {@code 404}.  Ids match {@code ^[A-Za-z0-9_-]{1,128}$}; anything else answers {@code 400}
 * before the resolver runs.  The mixin adds no auth of its own; the host resource's guards apply.
 *
 * <p>
 * Step events are not served here: pair the terminal with {@code RunViewMixin} over the same id.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>
 * 	<jk>public class</jk> RunsResource <jk>extends</jk> BasicRestServlet <jk>implements</jk> TerminalMixin, RunViewMixin {
 * 		<ja>@Override</ja>
 * 		<jk>public</jk> Optional&lt;TerminalSource&gt; terminalSource(String <jv>id</jv>, RestRequest <jv>req</jv>) {
 * 			<jk>return</jk> <jf>runs</jf>.find(<jv>id</jv>).map(Run::terminal);
 * 		}
 * 	}
 * </p>
 */
public interface TerminalMixin {

	/** The URL path prefix of the terminal endpoints (relative to the host mount). */
	String TERMINAL_PREFIX = "/juneau-terminal";

	/** The URL path template of the bytes endpoint. */
	String BYTES_PATH = TERMINAL_PREFIX + "/{id}/bytes";

	/** The URL path template of the raw download. */
	String RAW_PATH = TERMINAL_PREFIX + "/{id}/raw";

	/** The URL path of the terminal runtime. */
	String JS_PATH = "/juneau-terminal.js";

	/** The URL path of the terminal stylesheet. */
	String CSS_PATH = "/juneau-terminal.css";

	/** The {@code WebJarResolver} asset id of the xterm.js UMD bundle. */
	String XTERM_JS_ASSET = "org.webjars.npm:xterm__xterm:xterm__xterm/{version}/lib/xterm.js";

	/** The {@code WebJarResolver} asset id of the xterm.js stylesheet. */
	String XTERM_CSS_ASSET = "org.webjars.npm:xterm__xterm:xterm__xterm/{version}/css/xterm.css";

	/**
	 * Resolves the source for a terminal id, applying the application's own access rules.
	 *
	 * @param id The id from the URL path (already checked against the grammar).
	 * @param req The in-flight request.
	 * @return The source, or empty when the id is unknown or the caller may not read it.
	 */
	Optional<TerminalSource> terminalSource(String id, RestRequest req);

	/**
	 * [GET /juneau-terminal/{id}/bytes] &mdash; raw bytes from an offset.
	 *
	 * @param id The terminal id.
	 * @param req The request.
	 * @param res The response.
	 * @throws IOException If the response could not be written.
	 */
	@RestGet(path=BYTES_PATH, summary="Terminal bytes", swagger=@OpSwagger(ignore=true))
	default void terminalBytes(@Path("id") String id, RestRequest req, RestResponse res) throws IOException {
		TerminalEndpoints.bytes(terminalSource(TerminalEndpoints.checkId(id), req), req, res);
	}

	/**
	 * [GET /juneau-terminal/{id}/raw] &mdash; the whole log as an attachment.
	 *
	 * @param id The terminal id.
	 * @param req The request.
	 * @param res The response.
	 * @throws IOException If the response could not be written.
	 */
	@RestGet(path=RAW_PATH, summary="Terminal raw log", swagger=@OpSwagger(ignore=true))
	default void terminalRaw(@Path("id") String id, RestRequest req, RestResponse res) throws IOException {
		TerminalEndpoints.raw(terminalSource(TerminalEndpoints.checkId(id), req), id, res);
	}

	/**
	 * [GET /juneau-terminal.js] &mdash; the terminal runtime.
	 *
	 * @return The script.
	 */
	@RestGet(path=JS_PATH, summary="Juneau terminal runtime", swagger=@OpSwagger(ignore=true))
	default HttpResource terminalScript() {
		return TerminalEndpoints.script();
	}

	/**
	 * [GET /juneau-terminal.css] &mdash; the terminal stylesheet.
	 *
	 * @return The stylesheet.
	 */
	@RestGet(path=CSS_PATH, summary="Juneau terminal stylesheet", swagger=@OpSwagger(ignore=true))
	default HttpResource terminalStylesheet() {
		return TerminalEndpoints.stylesheet();
	}

	/**
	 * The absolute URL of a terminal asset; the toolkit pack resolver for the {@code terminal} pack.
	 *
	 * @param req The current request.
	 * @param path {@link #JS_PATH} or {@link #CSS_PATH}.
	 * @return The asset URL with its cache-buster.
	 */
	static String terminalAssetUrl(RestRequest req, String path) {
		return TerminalEndpoints.terminalAssetUrl(req, path);
	}
}
