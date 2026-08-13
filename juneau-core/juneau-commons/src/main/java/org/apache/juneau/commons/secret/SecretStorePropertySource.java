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
package org.apache.juneau.commons.secret;

import java.util.*;

import org.apache.juneau.commons.settings.*;

/**
 * Opt-in bridge that exposes a chosen {@link SecretStore} as a {@link PropertySource}, so a secret can be resolved
 * through the property/{@code $P{...}}/{@code @Value} machinery <b>only where a consumer deliberately wires it in</b>.
 *
 * <p>
 * By default a {@link SecretStore} is intentionally <b>not</b> part of the general config/SVL namespace &mdash; that is
 * what keeps a secret one careless {@code Settings} dump or debug log away from disclosure.  This adapter is the
 * conscious, greppable exception: attach it as a caller-scoped source (for example a session-scoped
 * {@code PropertySource[]} bean on a {@code VarResolverSession}) and its bridged keys become resolvable there, and
 * nowhere else.
 *
 * <p>
 * It implements {@link SensitivePropertySource} so any dump/log path that honors that marker redacts its values, and
 * its own {@link #toString()} never reveals the wrapped store or any secret.
 *
 * <p>
 * <b>Materialization trade-off.</b> The {@link PropertySource} contract returns values as {@link String}, so this
 * bridge necessarily converts the retrieved {@code char[]} into a {@link String} that cannot be explicitly zeroed and
 * may linger in the string pool.  That reintroduced exposure is precisely why bridging is opt-in and marked
 * sensitive rather than being the default way to reach a secret.
 *
 * @since 10.0.0
 */
public class SecretStorePropertySource implements SensitivePropertySource {

	private final SecretStore store;

	/**
	 * Constructor.
	 *
	 * @param store The secret store to expose.  Must not be <jk>null</jk>.
	 */
	public SecretStorePropertySource(SecretStore store) {
		this.store = Objects.requireNonNull(store, "store");
	}

	@Override /* PropertySource */
	public PropertyLookupResult get(String name) {
		if (name == null)
			return PropertyLookupResult.missing();
		return store.find(name)
			.map(v -> PropertyLookupResult.present(new String(v)))
			.orElseGet(PropertyLookupResult::missing);
	}

	@Override /* Object */
	public String toString() {
		return "SecretStorePropertySource(<redacted>)";
	}
}
