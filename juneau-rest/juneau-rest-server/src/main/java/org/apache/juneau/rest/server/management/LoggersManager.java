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
import java.util.logging.*;

import org.apache.juneau.rest.server.*;

/**
 * Shared worker for the {@code /loggers} management endpoint:  reads and sets
 * {@link java.util.logging java.util.logging} (JUL) levels at runtime.
 *
 * <p>
 * Both the {@link LoggersMixin mixin} and {@link LoggersResource resource} flavors delegate here, so the
 * two forms cannot drift &mdash; the same pattern the health package uses with its aggregator.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This endpoint is <b>JUL-only</b> in v1.  Applications that route logging through
 * 		SLF4J&rarr;Logback or Log4j2 will not have their levels changed here; backend-aware level control is
 * 		tracked as a separate follow-on.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class LoggersManager {

	/**
	 * Returns a snapshot of all known loggers and their <i>configured</i> levels.
	 *
	 * <p>
	 * Walks {@link LogManager#getLoggerNames()} and reports each logger's own level
	 * ({@link Logger#getLevel()}), or {@code null} (rendered as the empty string) when the logger inherits
	 * its level from an ancestor.  The root logger is reported under the key {@code "ROOT"}.
	 *
	 * @return A sorted map of logger name &rarr; configured level name (never <jk>null</jk>).
	 */
	public Map<String,String> getLevels() {
		var out = new TreeMap<String,String>();
		var lm = LogManager.getLogManager();
		var names = Collections.list(lm.getLoggerNames());
		for (var name : names) {
			var logger = lm.getLogger(name);
			if (logger == null)
				continue;
			var level = logger.getLevel();
			var key = name.isEmpty() ? "ROOT" : name;
			out.put(key, level == null ? "" : level.getName());
		}
		return out;
	}

	/**
	 * Returns the configured level of a single logger.
	 *
	 * @param name The logger name ({@code "ROOT"} or empty for the root logger).
	 * @return The configured level name, the empty string if the level is inherited, or <jk>null</jk> if no
	 * 	such logger is currently registered.
	 */
	public String getLevel(String name) {
		var logger = LogManager.getLogManager().getLogger(resolveName(name));
		if (logger == null)
			return null;
		var level = logger.getLevel();
		return level == null ? "" : level.getName();
	}

	/**
	 * Sets (or clears) the level of a single logger at runtime.
	 *
	 * <p>
	 * A non-null, non-blank {@code level} is parsed via {@link Level#parse(String)} and applied; a
	 * <jk>null</jk> or blank {@code level} clears the logger's own level so it inherits from its ancestor.
	 * The named logger is created on demand via {@link Logger#getLogger(String)} if it does not yet exist
	 * (matching JUL semantics).
	 *
	 * @param name The logger name ({@code "ROOT"} or empty for the root logger).
	 * @param level The level name (e.g. {@code "FINE"}, {@code "INFO"}, {@code "OFF"}), or <jk>null</jk>/blank to inherit.
	 * @throws IllegalArgumentException If {@code level} is non-blank but not a valid {@link Level} name.
	 */
	public void setLevel(String name, String level) {
		var logger = Logger.getLogger(resolveName(name));
		logger.setLevel(level == null || level.isBlank() ? null : Level.parse(level.trim()));
	}

	/**
	 * Resolves the {@link LoggersSettings} from the host context's bean store, falling back to the
	 * read-only default.
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

	private static String resolveName(String name) {
		// The root logger is the empty-string-named logger; expose it under the friendlier "ROOT" alias.
		return (name == null || name.equals("ROOT")) ? "" : name;
	}
}
