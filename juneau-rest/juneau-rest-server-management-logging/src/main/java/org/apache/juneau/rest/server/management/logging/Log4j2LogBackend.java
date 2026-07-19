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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.rest.server.management.*;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.config.*;

/**
 * {@link LogBackend} adapter for <a class="doclink" href="https://logging.apache.org/log4j/2.x/">Apache Log4j 2</a>,
 * for use behind the {@code /loggers} management endpoint.
 *
 * <p>
 * Drives the concrete Log4j2 {@link LoggerContext} / {@link Configuration} &mdash; <b>not</b> SLF4J the facade
 * (an SLF4J&rarr;Log4j2 binding still routes here because the emitting backend is Log4j2). Register it explicitly:
 *
 * <p class='bjava'>
 * 	<ja>@Bean</ja>
 * 	<jk>public</jk> LoggersSettings loggersSettings() {
 * 		<jk>return</jk> LoggersSettings.<jsm>create</jsm>().enableWrite().backend(<jk>new</jk> Log4j2LogBackend()).build();
 * 	}
 * </p>
 *
 * <h5 class='topic'>Level mapping</h5>
 *
 * <p>
 * Levels are Log4j2 standard level names ({@code "TRACE"}, {@code "DEBUG"}, {@code "INFO"}, {@code "WARN"},
 * {@code "ERROR"}, {@code "FATAL"}, {@code "OFF"}, {@code "ALL"}). Reads report each {@link LoggerConfig}'s
 * <b>configured</b> level. Sets are process-lifetime-only ({@link Configurator#setLevel(String, Level)}); the
 * root logger is addressed as {@code "ROOT"}.
 *
 * <p>
 * Log4j2 has no "inherited / no own level" sentinel the way JUL/Logback do &mdash; every {@link LoggerConfig} in
 * the configuration carries a concrete level &mdash; so reads return a level name (never the empty-string
 * "inherited" marker) for configured loggers, and {@code null} for a name that has no {@code LoggerConfig}.
 *
 * @since 10.0.0
 */
public class Log4j2LogBackend implements LogBackend {

	@SuppressWarnings({
		"resource" // Shared Log4j2 LoggerContext owned by LogManager/caller - not closed here.
	})
	private final LoggerContext context;

	/**
	 * Constructor using the current Log4j2 {@link LoggerContext}.
	 */
	public Log4j2LogBackend() {
		this((LoggerContext) LogManager.getContext(false));
	}

	/**
	 * Constructor with an explicit {@link LoggerContext} (for tests / multi-context setups).
	 *
	 * @param context The Log4j2 logger context. Must not be <jk>null</jk>.
	 */
	public Log4j2LogBackend(LoggerContext context) {
		this.context = Objects.requireNonNull(context, "context");
	}

	@Override /* LogBackend */
	public Map<String,String> getLevels() {
		var out = new TreeMap<String,String>();
		var cfg = context.getConfiguration();
		// The root LoggerConfig is NOT in getLoggers() (that map holds only the named, explicitly-configured
		// loggers) - it's reachable via getRootLogger().  Surface it under the "ROOT" key.
		var root = cfg.getRootLogger();
		if (root != null)
			out.put("ROOT", root.getLevel() == null ? "" : root.getLevel().name());
		for (var lc : cfg.getLoggers().values()) {
			if (lc.getName().isEmpty())
				continue;  // defensive: root already handled above
			out.put(lc.getName(), lc.getLevel() == null ? "" : lc.getLevel().name());
		}
		return out;
	}

	@Override /* LogBackend */
	public String getLevel(String name) {
		var cfg = context.getConfiguration();
		var resolved = resolveName(name);
		var lc = resolved.isEmpty() ? cfg.getRootLogger() : cfg.getLoggers().get(resolved);
		if (lc == null)
			return null;
		var level = lc.getLevel();
		return level == null ? "" : level.name();
	}

	@Override /* LogBackend */
	public void setLevel(String name, String level) {
		var target = (name == null || name.equals("ROOT")) ? LogManager.ROOT_LOGGER_NAME : name;
		// Null/blank clears the override back to the parent's level (Configurator treats null as "remove override").
		Configurator.setLevel(target, level == null || level.isBlank() ? null : parse(level.trim()));
	}

	private static Level parse(String level) {
		// Level.getLevel(name) returns null for an unknown name (Level.toLevel would silently default) - hard-fail
		// to match the JUL backend's IllegalArgumentException contract.
		var l = Level.getLevel(level.toUpperCase(Locale.ROOT));
		if (l == null)
			throw iaex("Not a valid Log4j2 level: '%s'.", level);
		return l;
	}

	private static String resolveName(String name) {
		return (name == null || name.equals("ROOT")) ? "" : name;
	}
}
