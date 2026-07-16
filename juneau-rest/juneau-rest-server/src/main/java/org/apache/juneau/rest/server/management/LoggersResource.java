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
package org.apache.juneau.rest.server.management;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Child-resource flavor of the {@code /loggers} runtime log-level management endpoint.
 *
 * <p>
 * Mounts as a routed child via {@link Rest#children() @Rest(children=LoggersResource.class)} under the
 * subtree {@code /loggers} and delegates to a shared {@link LoggersManager} worker &mdash; the same logic
 * the {@link LoggersMixin mixin} flavor uses, so the two forms cannot drift.  Extends
 * {@link BasicRestResource} so the returned beans serialize via the {@code BasicUniversalConfig} set
 * (mirrors {@code HealthResource}).
 *
 * <h5 class='section'>Backend:</h5><ul>
 * 	<li>Drives {@link JulLogBackend java.util.logging} by default; declare a Logback / Log4j2 backend
 * 		explicitly via {@link LoggersSettings.Builder#backend(LogBackend)} &mdash; see {@link LogBackend}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LoggersMixin}
 * 	<li class='jc'>{@link LoggersManager}
 * 	<li class='jc'>{@link LogBackend}
 * </ul>
 *
 * @since 10.0.0
 */
@Rest(path="/loggers")
public class LoggersResource extends BasicRestResource {

	private final LoggersManager manager = new LoggersManager();

	/**
	 * [GET /] - All loggers and their configured levels.
	 *
	 * @return A sorted map of logger name to configured level (empty string = inherited).
	 */
	@RestGet(
		path="/*",
		summary="Runtime logger levels",
		description="Lists all loggers and their configured levels (empty = inherited from ancestor)."
	)
	public Map<String,String> getLoggers(RestRequest req) {
		return manager.getLevels(req.getContext());
	}

	/**
	 * [GET /{name}] - One logger's configured level.
	 *
	 * @param req The HTTP request.
	 * @param name The logger name ("ROOT" for the root logger).
	 * @return The configured level name, empty string if inherited.
	 * @throws NotFound If no logger with that name is registered.
	 */
	@RestGet(
		path="/{name}",
		summary="Runtime logger level",
		description="Returns the configured level of a single logger."
	)
	public String getLogger(RestRequest req, @Path("name") String name) {
		var level = manager.getLevel(req.getContext(), name);
		if (level == null)
			throw new NotFound("No logger named '%s' is registered.", name);
		return level;
	}

	/**
	 * [PUT /{name}] - Set a logger's level at runtime.
	 *
	 * <p>
	 * <b>Sensitive (mutating) &mdash; deny-by-default:</b> responds {@code 403} unless a {@link LoggersSettings}
	 * bean with writes enabled is registered.
	 *
	 * @param req The HTTP request.
	 * @param name The logger name ("ROOT" for the root logger).
	 * @param level The level name (e.g. FINE, INFO, OFF), or blank to inherit from the ancestor.
	 * @return The newly-configured level (empty string if inherited).
	 * @throws Forbidden If the set-level endpoint is not enabled.
	 */
	@RestPut(
		path="/{name}",
		summary="Set logger level",
		description="Sets the java.util.logging level of a single logger at runtime (blank to inherit).  Disabled by default; opt in via LoggersSettings."
	)
	public String setLogger(RestRequest req, @Path("name") String name, @Content String level) {
		if (! manager.resolveSettings(req.getContext()).isWriteEnabled())
			throw new Forbidden("The /loggers set-level endpoint is disabled.  Register a LoggersSettings bean with write enabled to use it.");
		manager.setLevel(req.getContext(), name, level);
		// setLevel creates the logger on demand, so getLevel always returns non-null here (level name or "" if inherited).
		return manager.getLevel(req.getContext(), name);
	}

	/**
	 * [POST /{name}] - Set a logger's level at runtime (POST alias of {@link #setLogger(RestRequest,String,String)}).
	 *
	 * @param req The HTTP request.
	 * @param name The logger name ("ROOT" for the root logger).
	 * @param level The level name, or blank to inherit.
	 * @return The newly-configured level (empty string if inherited).
	 * @throws Forbidden If the set-level endpoint is not enabled.
	 */
	@RestPost(
		path="/{name}",
		summary="Set logger level",
		description="Sets the java.util.logging level of a single logger at runtime (blank to inherit).  Disabled by default; opt in via LoggersSettings."
	)
	public String setLoggerPost(RestRequest req, @Path("name") String name, @Content String level) {
		return setLogger(req, name, level);
	}
}
