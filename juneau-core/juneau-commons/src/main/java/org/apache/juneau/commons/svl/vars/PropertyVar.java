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
package org.apache.juneau.commons.svl.vars;

import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.*;

/**
 * Unified property variable resolver.
 *
 * <p>
 * The format for this var is <js>"$P{propertyName[,defaultValue]}"</js>.
 *
 * <h5 class='section'>Caller-scoped property sources:</h5>
 * <p>
 * Resolution consults the singleton {@link Settings} chain by default. Callers (notably the
 * {@code @Value} injection path) that need to inject additional, caller-scoped
 * {@link PropertySource}s in front of the global chain can attach them to the
 * {@link VarResolverSession} as a session bean of type {@code PropertySource[]}:
 *
 * <p class='bjava'>
 * 	<jv>session</jv>.bean(PropertySource[].<jk>class</jk>, <jk>new</jk> PropertySource[] { <jv>source1</jv>, <jv>source2</jv> });
 * </p>
 *
 * <p>
 * When the session carries that bean, this var consults the array in order between the
 * {@link Settings#getOverride(String) local/global override stores} (which still win &mdash;
 * matching the existing test-override contract) and the regular {@link Settings#get(String)
 * Settings sources chain}. Sources earlier in the array take precedence over sources later
 * in the array. The contract is identical to today when no such session bean is attached.
 */
public class PropertyVar extends DefaultingVar {

	/** The name of this variable. */
	public static final String NAME = "P";

	/**
	 * Constructor.
	 */
	public PropertyVar() {
		super(NAME);
	}

	@Override /* Overridden from Var */
	public String resolve(VarResolverSession session, String key) {
		var scoped = session == null ? null : session.getBean(PropertySource[].class).orElse(null);
		if (scoped == null || scoped.length == 0)
			return Settings.get().get(key).orElse(null);

		// Caller-scoped sources are present. Honor Settings local/global overrides first
		// so test-override semantics still win over caller-scoped sources, then walk the
		// caller-scoped chain, then fall through to the global sources list.
		if (Settings.get().isOverridden(key))
			return Settings.get().getOverride(key).orElse(null);
		for (var src : scoped) {
			if (src == null)
				continue;
			var r = src.get(key);
			if (r.isPresent())
				return r.value().orElse(null);
		}
		return Settings.get().get(key).orElse(null);
	}
}
