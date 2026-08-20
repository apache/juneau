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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * Serving-path allowlist of renderer ids that may be <em>named</em> on a fill sink
 * ({@link DetailField#render} or {@link PopoverField#render}).
 *
 * <p>
 * This does not freeze JavaScript functions; the client {@code resolveSinkRenderer} lookup owns that.  The
 * {@link Column#render} cell path does not call this helper (unknown ids still warn-and-fall-back).
 *
 * @since 10.0.0
 */
public final class SinkRenderAllowlist {

	/**
	 * Built-in ids safe to name on a {@link DetailField} fill sink (and the JS frozen-builtin set).
	 */
	public static final Set<String> BUILTIN_IDS = Set.of(
		"date", "datetime", "ts-zulu", "bool", "linked", "truncate", "json", "decimal", "tag", "progress"
	);

	/**
	 * Text-shaped built-ins allowed on a {@link PopoverField} (no tags in {@code display()} when popup is forced
	 * off).  A subset of {@link #BUILTIN_IDS}.
	 */
	public static final Set<String> POPOVER_TEXT_IDS = Set.of(
		"date", "bool", "decimal", "datetime", "ts-zulu"
	);

	private SinkRenderAllowlist() {}

	/**
	 * Accepts a frozen built-in id, or an id present in {@code allowedCustomIds}.
	 *
	 * @param renderId The renderer id.  Must not be <jk>null</jk> or blank.
	 * @param allowedCustomIds Opt-in custom ids (e.g. {@link RowDetailDef#allowCustomRenderers}).  Can be
	 * 	<jk>null</jk>.
	 * @throws IllegalArgumentException If the id is neither a built-in nor an opted-in custom id.
	 */
	public static void assertAllowed(String renderId, Collection<String> allowedCustomIds) {
		if (renderId == null || renderId.isBlank())
			throw iaex("Render id must not be null or blank.");
		if (BUILTIN_IDS.contains(renderId))
			return;
		if (allowedCustomIds != null && allowedCustomIds.contains(renderId))
			return;
		throw iaex("Render id '%s' is not an allowed fill-sink renderer.", renderId);
	}

	/**
	 * Accepts only the text-shaped popover built-ins.  Custom ids are never allowed on a popover.
	 *
	 * @param renderId The renderer id.  Must not be <jk>null</jk> or blank.
	 * @throws IllegalArgumentException If the id is not in {@link #POPOVER_TEXT_IDS}.
	 */
	public static void assertPopoverAllowed(String renderId) {
		if (renderId == null || renderId.isBlank())
			throw iaex("PopoverField render id must not be null or blank.");
		if (!POPOVER_TEXT_IDS.contains(renderId))
			throw iaex("PopoverField render id '%s' is not a text-shaped built-in.", renderId);
	}
}
