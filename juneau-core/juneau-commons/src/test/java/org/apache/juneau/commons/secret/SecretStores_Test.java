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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link SecretStores}: BeanStore-first resolution with an {@link InMemorySecretStore} drop-in default,
 * and opt-in {@code ServiceLoader} discovery via {@link SecretStoreProvider}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class SecretStores_Test extends TestBase {

	@Test void a01_resolveContributedStoreWins() {
		var contributed = new InMemorySecretStore();
		var beanStore = new BasicBeanStore().addBean(SecretStore.class, contributed);
		assertSame(contributed, SecretStores.resolve(beanStore));
	}

	@Test void a02_resolveEmptyBeanStoreReturnsInMemoryDefault() {
		var resolved = SecretStores.resolve(new BasicBeanStore());
		assertInstanceOf(InMemorySecretStore.class, resolved);
	}

	@Test void a03_resolveNullBeanStoreReturnsInMemoryDefault() {
		assertInstanceOf(InMemorySecretStore.class, SecretStores.resolve(null));
	}

	@Test void a04_fromServiceLoaderDiscoversRegisteredProvider() {
		// juneau-commons registers EnvVarSecretStoreProvider in META-INF/services, so discovery finds it.
		var discovered = SecretStores.fromServiceLoader();
		assertTrue(discovered.isPresent());
		assertInstanceOf(EnvVarSecretStore.class, discovered.orElseThrow());
	}

	@Test void a05_resolveIgnoresServiceLoaderSoDefaultStaysInMemory() {
		// The classpath has a registered provider, but resolve() must not consult ServiceLoader -- the
		// no-contribution default is a deterministic InMemorySecretStore, never the env-backed provider.
		assertInstanceOf(InMemorySecretStore.class, SecretStores.resolve(new BasicBeanStore()));
	}
}
