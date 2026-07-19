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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.ObjectUtils.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.logging.*;
import org.apache.juneau.commons.settings.*;

/**
 * System utilities.
 */
public class SystemUtils {

	private static final Logger LOG = Logger.getLogger(SystemUtils.class);

	/**
	 * Prevents instantiation.
	 */
	private SystemUtils() {}

	static final List<Supplier<String>> SHUTDOWN_MESSAGES = new CopyOnWriteArrayList<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (isFalse(Settings.get().get("juneau.shutdown.quiet").asBoolean().orElse(false))) // HTT - shutdown hook; true branch tested, false requires JVM shutdown with system property set
					SHUTDOWN_MESSAGES.forEach(x -> LOG.info(x.get()));
			}
		});
	}

	/**
	 * Adds a console message to display when the JVM shuts down.
	 *
	 * @param message The message to display.  Must not be <jk>null</jk> (evaluated at JVM shutdown).
	 */
	public static void shutdownMessage(Supplier<String> message) {
		SHUTDOWN_MESSAGES.add(message);
	}

	/**
	 * Looks up a system property or environment variable.
	 *
	 * @param name The property name.
	 * @return A StringSetting wrapping the value.
	 */
	public static StringSetting env(String name) {
		return Settings.get().get(name);
	}

	/**
	 * Looks up a system property or environment variable, returning a default if not found.
	 *
	 * @param <T> The type to convert the value to.
	 * @param name The property name.
	 * @param def The default value.  Can be <jk>null</jk> (returned as-is when the property is not found).
	 * @return The found value, or the default.
	 */
	public static <T> T env(String name, T def) {
		var s = Settings.get();
		return s != null ? s.get(name, def) : def;
	}
}