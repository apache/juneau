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
package org.apache.juneau.http;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Resolves the {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} environment-variable master gate for REST client debug
 * body output.
 *
 * <p>
 * Body dumping is off by default. The gate is resolved once from the environment and cached; a system property never
 * enables it.
 *
 * <p>
 * The value is trimmed first; a non-empty trimmed value enables the gate except case-insensitive {@code false} or
 * {@code 0}. A <jk>null</jk>, empty or all-whitespace value disables it.
 *
 * @since 10.0.0
 */
public final class RestDebugBodyGate {

	/** The environment variable that gates body dumping. */
	public static final String ENV_ALLOW_DUMP_BODIES = "JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES";

	private static final AtomicReference<Boolean> cache = new AtomicReference<>();

	private RestDebugBodyGate() {}

	/**
	 * Returns whether body dumping is enabled by the {@code JUNEAU_REST_DEBUG_ALLOW_DUMP_BODIES} environment variable.
	 *
	 * @return <jk>true</jk> if body dumping is enabled.
	 */
	public static boolean isAllowDumpBodies() {
		var v = cache.get();
		if (v == null) {
			v = parse(System.getenv(ENV_ALLOW_DUMP_BODIES));
			cache.set(v);
		}
		return v;
	}

	/**
	 * Parses a raw gate value.
	 *
	 * @param raw The raw environment value. Can be <jk>null</jk>.
	 * @return <jk>true</jk> if the value enables body dumping.
	 */
	public static boolean parse(String raw) {
		if (raw == null)
			return false;
		var t = raw.trim();
		if (t.isEmpty())
			return false;
		return ! (eqic(t, "false") || eq(t, "0"));
	}

	/**
	 * Overrides the cached gate state so tests can exercise both states without changing the process environment.
	 *
	 * <p>
	 * Not for application use.
	 *
	 * @param override {@code null} clears the cache so the next resolution re-reads the environment; a
	 * 	non-<jk>null</jk> value is cached directly.
	 */
	public static void setAllowDumpBodiesForTest(Boolean override) {
		cache.set(override);
	}
}
