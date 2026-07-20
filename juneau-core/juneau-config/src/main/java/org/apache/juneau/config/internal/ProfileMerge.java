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
package org.apache.juneau.config.internal;

import java.io.*;
import java.util.*;

import org.apache.juneau.config.format.*;
import org.apache.juneau.config.store.*;

/**
 * Merges a base configuration with one or more profile overlays into a single internal-INI string.
 *
 * <p>
 * This is the engine behind config <b>profiles</b> ({@code <name>-<profile>.cfg}/{@code .yml} overlays activated via
 * {@code juneau.profiles.active}).  The base config is the foundation; each active profile's entries are layered on
 * top in activation order so that:
 * <ul>
 * 	<li><b>profile wins over base</b> for any section/key the profile redefines, and
 * 	<li><b>last-active-profile wins</b> when multiple active profiles redefine the same section/key.
 * </ul>
 *
 * <p>
 * Each input (base + profiles) is parsed through its own {@link ConfigFormat} (so a YAML profile is converted to the
 * internal INI form like any other config), then the parsed maps are overlaid entry-by-entry and re-emitted as a
 * single internal-INI string via {@link ConfigMap#asIniString()}.  Feeding that merged string back through the normal
 * {@link ConfigMap} parse keeps format handling, variable resolution, and change-listener wiring identical to a
 * single-file config &mdash; profiles are transparent to everything downstream of the store.
 *
 * @since 10.0.0
 */
public final class ProfileMerge {

	private ProfileMerge() {}

	/**
	 * Merges base + profile contents into one internal-INI string (profile-wins, last-active-profile-wins).
	 *
	 * @param store The store the throwaway parse maps are associated with (used only for parse context; never written).
	 * @param baseName The base config name (for parse diagnostics).
	 * @param baseContents
	 * 	The raw base config contents (format-native).
	 * 	<br>Can be <jk>null</jk> (treated as empty contents).
	 * @param profiles
	 * 	Ordered profile overlays (activation order); each is a raw format-native contents string.
	 * 	<br>May be empty, but must not be <jk>null</jk>.
	 * @param format
	 * 	The config format used to parse every input.
	 * 	<br>Can be <jk>null</jk> (defaults to {@link IniConfigFormat}).
	 * @return The merged contents as an internal-INI string.
	 * @throws IOException If any input fails to parse.
	 */
	public static String merge(ConfigStore store, String baseName, String baseContents, List<String> profiles, ConfigFormat format) throws IOException {
		var f = format == null ? IniConfigFormat.INSTANCE : format;
		// Parse the base into a throwaway map; this becomes the accumulator we overlay onto.
		var merged = new ConfigMap(store, baseName, baseContents == null ? "" : baseContents, f);
		for (var p : profiles) {
			if (p == null || p.isBlank())
				continue;
			var overlay = new ConfigMap(store, baseName, p, f);
			applyOverlay(merged, overlay);
		}
		return merged.asIniString();
	}

	/**
	 * Overlays every section/key entry from {@code overlay} onto {@code base} (overlay wins).
	 */
	private static void applyOverlay(ConfigMap base, ConfigMap overlay) {
		for (var section : overlay.getSections()) {
			// Ensure the section exists on the base (preserving the overlay's section pre-lines when it is new).
			if (! base.hasSection(section))
				base.setSection(section, overlay.getPreLines(section));
			for (var key : overlay.getKeys(section)) {
				var e = overlay.getEntry(section, key);
				if (e != null)  // HTT: getKeys() only returns keys with entries, so getEntry is never null here.
					base.setEntry(section, key, e.getValue(), e.getModifiers(), e.getComment(), e.getPreLines());
			}
		}
	}
}
