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

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link SecretStoreProvider} discovery: the {@link EnvVarSecretStoreProvider} registered in
 * {@code META-INF/services} is found by {@code ServiceLoader}, and its {@code create()} yields an
 * {@link EnvVarSecretStore}.
 */
class SecretStoreProvider_Test extends TestBase {

	@Test void a01_serviceLoaderFindsEnvVarProvider() {
		var found = false;
		for (var p : ServiceLoader.load(SecretStoreProvider.class))
			if (p instanceof EnvVarSecretStoreProvider)
				found = true;
		assertTrue(found, "EnvVarSecretStoreProvider not discovered via ServiceLoader");
	}

	@Test void a02_envVarProviderCreatesEnvVarStore() {
		assertInstanceOf(EnvVarSecretStore.class, new EnvVarSecretStoreProvider().create());
	}

	@Test void a03_envVarProviderDefaultOrder() {
		assertEquals(30, new EnvVarSecretStoreProvider().order());
	}

	@Test void a04_defaultOrderIsZero() {
		SecretStoreProvider p = InMemorySecretStore::new;
		assertEquals(0, p.order());
	}
}
