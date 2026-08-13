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

import org.apache.juneau.commons.inject.*;

/**
 * Resolution helpers for selecting the active {@link SecretStore}.
 *
 * <p>
 * The active store is resolved from a {@link BeanStore}, defaulting to an {@link InMemorySecretStore} when none has
 * been contributed &mdash; the same {@code beanStore.getBean(...)}-with-drop-in-default idiom other framework-owned
 * components use.  An explicit {@link BeanStore} contribution always wins.
 *
 * <p>
 * {@link #fromServiceLoader()} is the separate classpath-contribution path, mirroring
 * {@link org.apache.juneau.commons.settings.Settings}'s opt-in {@code useServiceLoader()}.  It is deliberately
 * <b>not</b> consulted by {@link #resolve(BeanStore)}, so the no-contribution default stays a deterministic
 * {@link InMemorySecretStore} rather than silently becoming whatever store happens to be on the classpath (which, for
 * an environment-variable-backed provider, would quietly turn arbitrary env vars into secrets).  A consumer that
 * <i>wants</i> a classpath-contributed store can seed its {@link BeanStore} from {@link #fromServiceLoader()}.
 *
 * @since 10.0.0
 */
public final class SecretStores {

	private SecretStores() {}

	/**
	 * Resolves the active secret store from the specified bean store, defaulting to a new
	 * {@link InMemorySecretStore} when none is contributed.
	 *
	 * @param beanStore The bean store to resolve from.  Can be <jk>null</jk>, in which case the default is returned.
	 * @return The resolved store.  Never <jk>null</jk>.
	 */
	public static SecretStore resolve(BeanStore beanStore) {
		if (beanStore == null)
			return new InMemorySecretStore();
		return beanStore.getBean(SecretStore.class).orElseGet(InMemorySecretStore::new);
	}

	/**
	 * Discovers a secret store contributed on the classpath via {@link SecretStoreProvider} and {@code ServiceLoader}.
	 *
	 * <p>
	 * Providers are sorted by {@link SecretStoreProvider#order()} (lowest first) and the first one that yields a
	 * non-<jk>null</jk> store wins.
	 *
	 * @return The discovered store, or {@link Optional#empty()} if none is registered on the classpath.
	 */
	public static Optional<SecretStore> fromServiceLoader() {
		return ServiceLoader.load(SecretStoreProvider.class).stream()
			.map(ServiceLoader.Provider::get)
			.sorted(Comparator.comparingInt(SecretStoreProvider::order))
			.map(SecretStoreProvider::create)
			.filter(Objects::nonNull)
			.findFirst();
	}
}
