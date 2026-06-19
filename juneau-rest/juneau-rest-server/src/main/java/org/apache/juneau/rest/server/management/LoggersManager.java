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

import org.apache.juneau.rest.server.*;

/**
 * Shared worker for the {@code /loggers} management endpoint:  reads and sets logger levels at runtime by
 * delegating to the {@link LogBackend} resolved from the host context.
 *
 * <p>
 * Both the {@link LoggersMixin mixin} and {@link LoggersResource resource} flavors delegate here, so the
 * two forms cannot drift &mdash; the same pattern the health package uses with its aggregator.
 *
 * <h5 class='section'>Backend</h5>
 * <p>
 * The driven backend defaults to {@link JulLogBackend java.util.logging}.  A consumer drives a different backend
 * (e.g. Logback / Log4j2 via {@code juneau-rest-server-management-logging}) by <i>explicitly</i> declaring it on a
 * {@link LoggersSettings} bean via {@link LoggersSettings.Builder#backend(LogBackend)} &mdash; the endpoint never
 * auto-detects.  The response shape (logger name &rarr; configured level, empty string = inherited) is identical
 * across backends.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LogBackend}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class LoggersManager {

	/**
	 * Returns a snapshot of all known loggers and their configured levels, for the backend resolved from the
	 * supplied context.
	 *
	 * @param context The REST context whose bean store supplies the {@link LoggersSettings} / {@link LogBackend}.
	 * 	May be <jk>null</jk> (uses the JUL default).
	 * @return A sorted map of logger name &rarr; configured level name (empty string = inherited).  Never <jk>null</jk>.
	 */
	public Map<String,String> getLevels(RestContext context) {
		return resolveSettings(context).getBackend().getLevels();
	}

	/**
	 * Returns the configured level of a single logger, for the backend resolved from the supplied context.
	 *
	 * @param context The REST context whose bean store supplies the backend.  May be <jk>null</jk>.
	 * @param name The logger name ({@code "ROOT"} or empty for the root logger).
	 * @return The configured level name, the empty string if the level is inherited, or <jk>null</jk> if no
	 * 	such logger is currently registered.
	 */
	public String getLevel(RestContext context, String name) {
		return resolveSettings(context).getBackend().getLevel(name);
	}

	/**
	 * Sets (or clears) the level of a single logger at runtime (process-lifetime-only), for the backend resolved
	 * from the supplied context.
	 *
	 * @param context The REST context whose bean store supplies the backend.  May be <jk>null</jk>.
	 * @param name The logger name ({@code "ROOT"} or empty for the root logger).
	 * @param level The backend-native level name (e.g. {@code "FINE"}/{@code "INFO"} for JUL,
	 * 	{@code "DEBUG"}/{@code "INFO"} for Logback/Log4j2), or <jk>null</jk>/blank to inherit.
	 * @throws IllegalArgumentException If {@code level} is non-blank but not a valid level name for the backend.
	 */
	public void setLevel(RestContext context, String name, String level) {
		resolveSettings(context).getBackend().setLevel(name, level);
	}

	/**
	 * Resolves the {@link LoggersSettings} from the host context's bean store, falling back to the
	 * read-only (JUL-backed) default.
	 *
	 * @param context The REST context whose bean store is searched.  May be <jk>null</jk>.
	 * @return The registered settings, or {@link LoggersSettings#DEFAULT} when none is registered.
	 */
	@SuppressWarnings({
		"resource" // The bean store is owned by the RestContext; this adapter only borrows beans and must not close it.
	})
	public LoggersSettings resolveSettings(RestContext context) {
		if (context == null)
			return LoggersSettings.DEFAULT;
		return context.getBeanStore().getBean(LoggersSettings.class).orElse(LoggersSettings.DEFAULT);
	}
}
