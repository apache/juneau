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
 * Mixin flavor of the {@code /loggers} runtime log-level management endpoint (JUL).
 *
 * <p>
 * Composes the logger read/set endpoints into a host resource via
 * {@link Rest#mixins() @Rest(mixins=LoggersMixin.class)}.  Delegates to a shared {@link LoggersManager}
 * worker so the mixin and {@link LoggersResource resource} flavors cannot drift.
 *
 * <p>
 * The {@code GET} endpoints (list all / read one) are read-only and safe to expose; the {@code PUT}/{@code POST}
 * set-level endpoint <b>mutates runtime logging</b> and should be guarded (deny-by-default) when assembled into a
 * management group &mdash; see the actuator group's exposure policy.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>JUL-only in v1 &mdash; see {@link LoggersManager}.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LoggersResource}
 * 	<li class='jc'>{@link LoggersManager}
 * </ul>
 *
 * @since 10.0.0
 */
@Rest
public class LoggersMixin extends RestMixin {

	private final LoggersManager manager = new LoggersManager();

	/**
	 * [GET /loggers] - All loggers and their configured levels.
	 *
	 * @return A sorted map of logger name to configured level (empty string = inherited).
	 */
	@RestGet(
		path="/loggers",
		summary="Runtime logger levels",
		description="Lists all java.util.logging loggers and their configured levels (empty = inherited from ancestor)."
	)
	public Map<String,String> getLoggers() {
		return manager.getLevels();
	}

	/**
	 * [GET /loggers/{name}] - One logger's configured level.
	 *
	 * @param name The logger name ("ROOT" for the root logger).
	 * @return The configured level name, empty string if inherited.
	 * @throws NotFound If no logger with that name is registered.
	 */
	@RestGet(
		path="/loggers/{name}",
		summary="Runtime logger level",
		description="Returns the configured level of a single java.util.logging logger."
	)
	public String getLogger(@Path("name") String name) {
		var level = manager.getLevel(name);
		if (level == null)
			throw new NotFound("No logger named ''{0}'' is registered.", name);
		return level;
	}

	/**
	 * [PUT /loggers/{name}] - Set a logger's level at runtime.
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
		path="/loggers/{name}",
		summary="Set logger level",
		description="Sets the java.util.logging level of a single logger at runtime (blank to inherit).  Disabled by default; opt in via LoggersSettings."
	)
	public String setLogger(RestRequest req, @Path("name") String name, @Content String level) {
		if (! manager.resolveSettings(req.getContext()).isWriteEnabled())
			throw new Forbidden("The /loggers set-level endpoint is disabled.  Register a LoggersSettings bean with write enabled to use it.");
		manager.setLevel(name, level);
		// setLevel creates the logger on demand, so getLevel always returns non-null here (level name or "" if inherited).
		return manager.getLevel(name);
	}

	/**
	 * [POST /loggers/{name}] - Set a logger's level at runtime (POST alias of {@link #setLogger(RestRequest,String,String)}).
	 *
	 * @param req The HTTP request.
	 * @param name The logger name ("ROOT" for the root logger).
	 * @param level The level name, or blank to inherit.
	 * @return The newly-configured level (empty string if inherited).
	 * @throws Forbidden If the set-level endpoint is not enabled.
	 */
	@RestPost(
		path="/loggers/{name}",
		summary="Set logger level",
		description="Sets the java.util.logging level of a single logger at runtime (blank to inherit).  Disabled by default; opt in via LoggersSettings."
	)
	public String setLoggerPost(RestRequest req, @Path("name") String name, @Content String level) {
		return setLogger(req, name, level);
	}
}
