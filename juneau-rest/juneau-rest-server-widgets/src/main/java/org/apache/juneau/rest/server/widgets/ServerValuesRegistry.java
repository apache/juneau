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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.svl.*;

/**
 * An internal, per-render resolver for a single {@link ServerValues} declaration.
 *
 * <p>
 * A registry is added to a <b>sibling</b> {@link VarResolverSession} bean store so provider values never leak
 * across requests.  {@code ServerValuesVar} ({@code $FV}) looks this bean up in the session and delegates each
 * <js>"$FV{name}"</js> resolution to {@link #resolve(VarResolverSession, String)}.
 *
 * <p>
 * Resolution rejects non-scalar provider results (no {@code List.toString()} join) and memoizes only entries
 * flagged {@link ServerValuesValue#cacheable}.  A missing name returns <jk>null</jk> so the caller's
 * {@code DefaultingVar} default applies; a provider that throws is allowed to propagate (fail-closed).
 *
 * @since 10.0.0
 */
public class ServerValuesRegistry {

	private final Map<String,ServerValuesValue> values;
	private final Map<String,String> memo = new HashMap<>();

	/**
	 * Constructor.
	 *
	 * @param values The value declarations, keyed by interpolation name.
	 */
	private ServerValuesRegistry(Map<String,ServerValuesValue> values) {
		this.values = values == null ? Collections.emptyMap() : values;
	}

	/**
	 * Creates a per-render registry over the specified declaration.
	 *
	 * @param serverValues The declaration.  Must not be <jk>null</jk>.
	 * @return A new {@link ServerValuesRegistry}.
	 */
	public static ServerValuesRegistry of(ServerValues serverValues) {
		return new ServerValuesRegistry(serverValues.values);
	}

	/**
	 * Resolves a single named scalar value.
	 *
	 * @param session The per-render session passed to the provider.
	 * @param name The interpolation name (first {@code DefaultingVar} field only).
	 * @return The resolved scalar as a string, or <jk>null</jk> if the name is not declared or the provider
	 * 	returned <jk>null</jk> (so a {@code DefaultingVar} default applies).
	 */
	public String resolve(VarResolverSession session, String name) {
		var v = values.get(name);
		if (v == null)
			return null;
		if (v.cacheable && memo.containsKey(name))
			return memo.get(name);
		var o = v.provider.apply(session);
		if (o == null)
			return null;
		if (!(o instanceof String || o instanceof Number || o instanceof Boolean))
			throw iaex("ServerValues provider ''{0}'' returned non-scalar type {1}.", name, o.getClass().getName());
		var s = o.toString();
		if (v.cacheable)
			memo.put(name, s);
		return s;
	}
}
