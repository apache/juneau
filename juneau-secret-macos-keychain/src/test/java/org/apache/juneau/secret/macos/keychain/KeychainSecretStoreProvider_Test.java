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
package org.apache.juneau.secret.macos.keychain;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.commons.secret.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link KeychainSecretStoreProvider}: discoverable via {@code ServiceLoader}, macOS-gated
 * {@code create()}, and a fixed order.
 */
class KeychainSecretStoreProvider_Test {

	private static boolean isMac() {
		return System.getProperty("os.name", "").toLowerCase().contains("mac");
	}

	@Test void a01_serviceLoaderFindsProvider() {
		var found = false;
		for (var p : ServiceLoader.load(SecretStoreProvider.class))
			if (p instanceof KeychainSecretStoreProvider)
				found = true;
		assertTrue(found, "KeychainSecretStoreProvider not discovered via ServiceLoader");
	}

	@Test void a02_createOnMacYieldsKeychainStore() {
		assumeTrue(isMac(), "Not macOS");
		assertInstanceOf(KeychainSecretStore.class, new KeychainSecretStoreProvider().create());
	}

	@Test void a03_createOnNonMacReturnsNull() {
		assumeFalse(isMac(), "Only meaningful off macOS");
		assertNull(new KeychainSecretStoreProvider().create());
	}

	@Test void a04_order() {
		assertEquals(10, new KeychainSecretStoreProvider().order());
	}
}
