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
 * Serving-path allowlist of populator names that may be named on a region ({@link RegionDef#populate}).
 *
 * <p>
 * This is an <b>authoring-discipline control, not a security boundary</b>, and the reason is structural, not a
 * weaker policy choice.  {@link SinkRenderAllowlist} is real defense: a named renderer's markup still has to pass
 * through the closed {@code fillRenderSlot} copier, so an accepted id only ever gets to write what that copier lets
 * it write. A populator has no such copier anywhere in its path &mdash; {@code JuneauViews.regions.resolve(name)}
 * hands the resolved function the region's container element outright, and from that point the populator owns the
 * container completely. This class therefore controls only <i>which name is resolved</i>; it says nothing whatever
 * about what the resolved populator then does with the container it is handed. A reader who transfers
 * {@link RowDetailDef#allowCustomRenderers}'s security value onto this allowlist will over-trust it.
 *
 * @since 10.0.0
 */
public final class PopulatorAllowlist {

	/**
	 * The reserved declarative-default populator name &mdash; the only Juneau-shipped populator today.
	 *
	 * <p>
	 * Unlike an ordinary {@code JuneauViews.regions.register(name, fn)} entry, which is last-write-wins and freely
	 * overridable by a later registration under the same name, the client registry refuses to let a consumer
	 * override the entry named <js>"default"</js>: that name is reserved for the framework's own declarative-default
	 * populator.
	 */
	public static final Set<String> BUILTIN_IDS = Set.of("default");

	private PopulatorAllowlist() {}

	/**
	 * Accepts a reserved built-in name, or a name present in {@code allowedNames}.
	 *
	 * @param populatorName The populator name.  Must not be <jk>null</jk> or blank.
	 * @param allowedNames Opt-in populator names (e.g. {@link RegionDef#allowedPopulators}).  Can be <jk>null</jk>.
	 * @throws IllegalArgumentException If the name is neither a built-in nor an opted-in name.
	 */
	public static void assertAllowed(String populatorName, Collection<String> allowedNames) {
		if (populatorName == null || populatorName.isBlank())
			throw iaex("Populator name must not be null or blank.");
		if (BUILTIN_IDS.contains(populatorName))
			return;
		if (allowedNames != null && allowedNames.contains(populatorName))
			return;
		throw iaex("Populator name '%s' is not an allowed region populator.", populatorName);
	}
}
