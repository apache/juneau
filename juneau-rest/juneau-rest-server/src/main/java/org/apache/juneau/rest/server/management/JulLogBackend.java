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

/**
 * {@link LogBackend} implementation for {@link java.util.logging java.util.logging} (JUL).
 *
 * <p>
 * This is the default backend driven by {@link LoggersManager} when no {@link LoggersSettings} declares another
 * one, and it carries the original v1 {@code /loggers} behavior so the JUL path is unchanged: reads report each
 * logger's own ({@link Logger#getLevel() configured}) level (empty string when inherited), the root logger is
 * keyed {@code "ROOT"}, and sets are process-lifetime-only via {@link Logger#setLevel(Level)}.
 *
 * @since 10.0.0
 */
public class JulLogBackend implements LogBackend {

	/** Process-wide shared instance (stateless). */
	public static final JulLogBackend INSTANCE = new JulLogBackend();

	@Override /* LogBackend */
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

	@Override /* LogBackend */
	public String getLevel(String name) {
		var logger = LogManager.getLogManager().getLogger(resolveName(name));
		if (logger == null)
			return null;
		var level = logger.getLevel();
		return level == null ? "" : level.getName();
	}

	@Override /* LogBackend */
	public void setLevel(String name, String level) {
		var logger = Logger.getLogger(resolveName(name));
		logger.setLevel(level == null || level.isBlank() ? null : Level.parse(level.trim()));
	}

	private static String resolveName(String name) {
		// The root logger is the empty-string-named logger; expose it under the friendlier "ROOT" alias.
		return (name == null || name.equals("ROOT")) ? "" : name;
	}
}
