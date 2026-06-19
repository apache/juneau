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
package org.apache.juneau.rest.server.management.logging;

import java.util.*;

import org.apache.juneau.rest.server.management.*;
import org.slf4j.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * {@link LogBackend} adapter for <a class="doclink" href="https://logback.qos.ch">Logback</a> (the SLF4J native
 * backend), for use behind the {@code /loggers} management endpoint.
 *
 * <p>
 * Drives the concrete Logback {@link LoggerContext} &mdash; <b>not</b> SLF4J the facade. Register it explicitly:
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> LoggersSettings loggersSettings() {
 * 		<jk>return</jk> LoggersSettings.<jsm>create</jsm>().enableWrite().backend(<jk>new</jk> LogbackLogBackend()).build();
 * 	}
 * </p>
 *
 * <h5 class='topic'>Level mapping</h5>
 *
 * <p>
 * Levels are Logback level names ({@code "TRACE"}, {@code "DEBUG"}, {@code "INFO"}, {@code "WARN"},
 * {@code "ERROR"}, {@code "OFF"}). Reads report each logger's <b>configured</b> level (the level set on the logger
 * itself, or the empty string when it inherits from an ancestor). Sets are process-lifetime-only
 * ({@code Logger.setLevel(...)}); the root logger is addressed as {@code "ROOT"}.
 *
 * @since 10.0.0
 */
public class LogbackLogBackend implements LogBackend {

	private final LoggerContext context;

	/**
	 * Constructor using the SLF4J-bound Logback {@link LoggerContext}.
	 *
	 * @throws IllegalStateException If the bound SLF4J backend is not Logback.
	 */
	public LogbackLogBackend() {
		var factory = LoggerFactory.getILoggerFactory();
		if (! (factory instanceof LoggerContext lc))
			throw new IllegalStateException(
				"The bound SLF4J backend is not Logback (ILoggerFactory=" + factory.getClass().getName()
				+ ").  /loggers drives the concrete backend, not the SLF4J facade.");
		this.context = lc;
	}

	/**
	 * Constructor with an explicit {@link LoggerContext} (for tests / multi-context setups).
	 *
	 * @param context The Logback logger context. Must not be <jk>null</jk>.
	 */
	public LogbackLogBackend(LoggerContext context) {
		this.context = Objects.requireNonNull(context, "context");
	}

	@Override /* LogBackend */
	public Map<String,String> getLevels() {
		var out = new TreeMap<String,String>();
		for (var logger : context.getLoggerList()) {
			var level = logger.getLevel();  // configured level; null = inherited
			var name = logger.getName();
			var key = Logger.ROOT_LOGGER_NAME.equals(name) ? "ROOT" : name;
			out.put(key, level == null ? "" : level.toString());
		}
		return out;
	}

	@Override /* LogBackend */
	public String getLevel(String name) {
		// Logback exists-check: getLogger(...) creates on demand, so probe the existing list first.
		var resolved = resolveName(name);
		for (var logger : context.getLoggerList())
			if (logger.getName().equals(resolved)) {
				var level = logger.getLevel();
				return level == null ? "" : level.toString();
			}
		return null;
	}

	@Override /* LogBackend */
	public void setLevel(String name, String level) {
		var logger = context.getLogger(resolveName(name));
		// A null/blank level clears the logger's own level so it inherits from its ancestor.
		// Level.toLevel(null) returns DEBUG, so guard the inherit case explicitly.
		logger.setLevel(level == null || level.isBlank() ? null : parse(level.trim()));
	}

	private static Level parse(String level) {
		// Logback's Level.toLevel(name, default) silently falls back; we want a hard failure on a bad name to
		// match the JUL backend's IllegalArgumentException contract.
		var l = Level.toLevel(level, null);
		if (l == null)
			throw new IllegalArgumentException("Not a valid Logback level: '" + level + "'.");
		return l;
	}

	private static String resolveName(String name) {
		return (name == null || name.equals("ROOT")) ? Logger.ROOT_LOGGER_NAME : name;
	}
}
