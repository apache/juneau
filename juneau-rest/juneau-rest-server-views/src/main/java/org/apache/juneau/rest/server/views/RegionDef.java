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
 * A page "region": a named container whose contents a client-side populator fills in, chosen by name via
 * {@link #populate} rather than declared inline.
 *
 * <p>
 * This type is deliberately minimal.  A region's full descriptor shape &mdash; its data source, request parameters,
 * an inline renderer, lazy activation, a refresh interval, and a field/title projection &mdash; together with the
 * sidecar it projects into, is a separate, larger contract that a later revision of this type owns.  This first
 * pass carries only what is needed to name a region, select a populator for it by name, and validate that selection
 * against the server-side allowlist.  Do not add data-source, refresh, or field-projection fields here; they belong
 * to that larger contract, not to this one.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class RegionDef {

	/**
	 * The frozen contract version for the region envelope.
	 *
	 * <p>
	 * Deliberately independent of {@link ViewDef#CONTRACT_VERSION} and {@link RowDetailDef#CONTRACT_VERSION}, for
	 * the same reason those two envelopes are independent of each other: a region-contract revision must never
	 * force a view- or detail-sidecar bump, or vice-versa.
	 */
	public static final String CONTRACT_VERSION = "1";

	/** This region's own id, unique within its host. */
	public String id;

	/**
	 * The name of the client populator to resolve via {@code JuneauViews.regions.resolve(name)}, or <jk>null</jk>
	 * for the reserved declarative default (<js>"default"</js>, see {@link PopulatorAllowlist#BUILTIN_IDS}).
	 *
	 * <p>
	 * This is a name looked up in the client registry rather than an inline function for two reasons: an inline
	 * function cannot survive the JSON sidecar this envelope travels in, and a name &mdash; unlike an arbitrary
	 * function body &mdash; is the one shape a server-side allowlist ({@link PopulatorAllowlist}) can meaningfully
	 * constrain at all.
	 */
	public String populate;

	/**
	 * App-approved populator names allowed on {@link #populate} in addition to
	 * {@link PopulatorAllowlist#BUILTIN_IDS}.  Blank entries fail {@link #validate()}.
	 *
	 * <p>
	 * Opting a name in here is authoring discipline, not a security boundary &mdash; see
	 * {@link PopulatorAllowlist}'s class Javadoc.
	 */
	public Set<String> allowedPopulators;

	/**
	 * Creates a region with the given id.
	 *
	 * @param id This region's own id, unique within its host.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link RegionDef}.
	 */
	public static RegionDef create(String id) {
		var r = new RegionDef();
		r.id = id;
		return r;
	}

	/**
	 * Sets the name of the client populator to resolve.
	 *
	 * @param value A populator name, or <jk>null</jk> to use the reserved declarative default.
	 * @return This object.
	 */
	public RegionDef populate(String value) {
		populate = value;
		return this;
	}

	/**
	 * Opts in app-approved populator names for {@link #populate}, in addition to
	 * {@link PopulatorAllowlist#BUILTIN_IDS}.
	 *
	 * @param value Populator names.  Must not contain blank entries.
	 * @return This object.
	 */
	public RegionDef allowPopulators(String...value) {
		allowedPopulators = st(value);
		return this;
	}

	/**
	 * Fail-closed bean validation: rejects a missing/blank {@link #id}, a blank {@link #allowedPopulators} entry,
	 * and (when {@link #populate} is set) a populator name that {@link PopulatorAllowlist} does not allow.
	 *
	 * <p>
	 * The populator-name check happens here, at startup, rather than at paint time.  The client runtime fails
	 * closed on a name it cannot resolve, which is the right runtime behaviour and the wrong startup behaviour: a
	 * typo left to surface there silently disables the region on every page that reaches it, with no one watching
	 * for the console warning.  So the typo fails loud here instead, at the one point that can see both the
	 * declared name and the allowlist.
	 *
	 * @throws IllegalArgumentException If this definition is not well-formed.
	 */
	public void validate() {
		if (id == null || id.isBlank())
			throw iaex("RegionDef id must not be null or blank.");
		if (allowedPopulators != null)
			for (var name : allowedPopulators)
				if (name == null || name.isBlank())
					throw iaex("allowPopulators entry must not be blank.");
		if (populate != null)
			PopulatorAllowlist.assertAllowed(populate, allowedPopulators);
	}
}
