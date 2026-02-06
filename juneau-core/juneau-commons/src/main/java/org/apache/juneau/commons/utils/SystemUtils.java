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

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.logging.Logger;

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
				if (! Boolean.getBoolean("juneau.shutdown.quiet"))
					SHUTDOWN_MESSAGES.forEach(x -> LOG.info(x.get()));
			}
		});
	}

	/**
	 * Adds a console message to display when the JVM shuts down.
	 *
	 * @param message The message to display.
	 */
	public static void shutdownMessage(Supplier<String> message) {
		SHUTDOWN_MESSAGES.add(message);
	}
}