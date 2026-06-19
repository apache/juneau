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

/**
 * SPI behind the {@code /loggers} management endpoint: reads and sets logger levels for one concrete logging
 * backend at runtime.
 *
 * <p>
 * {@code juneau-rest-server} ships only the {@link JulLogBackend java.util.logging} implementation, keeping the
 * core management surface dependency-free. Support for other backends is opt-in: the
 * {@code juneau-rest-server-management-logging} add-on ships {@code LogbackLogBackend} / {@code Log4j2LogBackend}
 * (with the backends declared {@code provided}), and any consumer can implement this interface for a backend
 * Juneau does not cover.
 *
 * <h5 class='topic'>Explicit selection (no auto-drive)</h5>
 *
 * <p>
 * Consistent with Juneau's explicit-over-magic stance, the backend is <b>explicitly declared</b> by registering a
 * {@link LoggersSettings} bean built with {@link LoggersSettings.Builder#backend(LogBackend)}. When none is
 * declared, {@link LoggersManager} drives {@link JulLogBackend} &mdash; the endpoint never silently classpath-scans
 * for, and then drives, a backend the operator didn't choose.
 *
 * <h5 class='topic'>Level contract</h5>
 *
 * <p>
 * Levels are exchanged as backend-native level-name strings (e.g. JUL {@code "FINE"}/{@code "INFO"}, Logback /
 * Log4j2 {@code "DEBUG"}/{@code "INFO"}). Reads report the logger's <b>configured</b> level &mdash; the level set
 * on the logger itself, or the empty string when it inherits from an ancestor. Sets are
 * <b>process-lifetime-only</b> (they do not rewrite the backend's configuration file). The root logger is
 * addressed under the key {@code "ROOT"}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link JulLogBackend}
 * 	<li class='jc'>{@link LoggersManager}
 * 	<li class='jc'>{@link LoggersSettings}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ManagementSurface">Management Surface</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface LogBackend {

	/**
	 * Returns a snapshot of all known loggers and their configured levels.
	 *
	 * @return A sorted map of logger name &rarr; configured level name (empty string = inherited from an
	 * 	ancestor). The root logger is keyed {@code "ROOT"}. Never <jk>null</jk>.
	 */
	Map<String,String> getLevels();

	/**
	 * Returns the configured level of a single logger.
	 *
	 * @param name The logger name ({@code "ROOT"} or empty for the root logger).
	 * @return The configured level name, the empty string if the level is inherited, or <jk>null</jk> if no such
	 * 	logger is currently registered.
	 */
	String getLevel(String name);

	/**
	 * Sets (or clears) the level of a single logger at runtime (process-lifetime-only).
	 *
	 * @param name The logger name ({@code "ROOT"} or empty for the root logger).
	 * @param level The backend-native level name, or <jk>null</jk>/blank to clear the logger's own level so it
	 * 	inherits from its ancestor.
	 * @throws IllegalArgumentException If {@code level} is non-blank but not a valid level name for the backend.
	 */
	void setLevel(String name, String level);
}
