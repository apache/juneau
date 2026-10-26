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
package org.apache.juneau.console.freemarker;

import java.util.*;
import java.util.regex.*;

/**
 * A rail section of quick time-range presets, with an optional custom from/to.
 *
 * <p>
 * Publishes {@code {schemaVersion:1, viewId, options:{range}, start, end}} on its {@code filter:} topic, where
 * {@code range} is the preset or {@code "custom"} and {@code start}/{@code end} are ISO-8601 UTC instants.
 * {@code Nd} and {@code Nh} end at now floored to the minute; {@code ytd} starts at January 1, 00:00 UTC.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RangeSection.<jsm>quick</jsm>(<js>"7d"</js>, <js>"30d"</js>, <js>"90d"</js>, <js>"ytd"</js>).custom(<jk>true</jk>).defaultPreset(<js>"30d"</js>)
 * 		.publishes(Topics.<jsm>filter</jsm>(<js>"range"</js>));
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Repeated literals are contract wire keys; named constants would obscure the contract shape.
})
public final class RangeSection extends Section {

	private static final Pattern PRESET = Pattern.compile("^([1-9]\\d{0,3}[dh]|ytd)$");

	private final List<String> presets;
	private boolean custom;
	private String defaultPreset;

	private RangeSection(String...presets) {
		this.presets = Arrays.asList(presets);  // A null preset is reported by check(), not an NPE here.
	}

	/**
	 * Creates a range section.
	 *
	 * @param presets The quick presets, each {@code Nd}, {@code Nh} or {@code ytd}.
	 * @return A new section.
	 */
	public static RangeSection quick(String...presets) {
		return new RangeSection(presets);
	}

	/**
	 * Adds the custom from/to inputs (read as UTC).
	 *
	 * @param value <jk>true</jk> to show them.
	 * @return This object.
	 */
	public RangeSection custom(boolean value) {
		custom = value;
		return this;
	}

	/**
	 * Sets the preset used when nothing is restored.
	 *
	 * @param value One of the presets.  Defaults to the first.
	 * @return This object.
	 */
	public RangeSection defaultPreset(String value) {
		defaultPreset = value;
		return this;
	}

	@Override /* Section */
	public RangeSection publishes(String topic) {
		super.publishes(topic);
		return this;
	}

	@Override /* Section */
	public RangeSection label(String label) {
		super.label(label);
		return this;
	}

	@Override String kind() { return "range"; }

	@Override void check(String dash) {
		super.check(dash);
		if (presets.isEmpty())
			throw e77(dash, "range section '" + id() + "' needs at least one preset");
		var seen = new HashSet<String>();
		for (var p : presets) {
			if (p == null || ! PRESET.matcher(p).matches())
				throw e77(dash, "range section '" + id() + "' preset '" + p + "' is not Nd, Nh or ytd");
			if (! seen.add(p))
				throw e77(dash, "range section '" + id() + "' lists preset '" + p + "' twice");
		}
		if (defaultPreset != null && ! presets.contains(defaultPreset))
			throw e77(dash, "range section '" + id() + "' default preset '" + defaultPreset + "' is not one of its presets");
	}

	@Override void body(Map<String,Object> m) {
		m.put("presets", presets);
		m.put("custom", custom);
		m.put("defaultPreset", defaultPreset != null ? defaultPreset : presets.get(0));
	}
}
