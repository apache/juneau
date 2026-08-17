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
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link EnvVarSecretStore}: find/exists resolve from the process environment, and the mutating
 * operations are unsupported (read-only).
 */
class EnvVarSecretStore_Test extends TestBase {

	/** Returns the name of some environment variable that is set in this process, or empty if the environment is empty. */
	private static Optional<String> anyEnvVar() {
		return System.getenv().keySet().stream().findFirst();
	}

	@Test void a01_findExistingEnvVar() {
		var name = anyEnvVar();
		assumeTrue(name.isPresent(), "No environment variables available");
		var store = new EnvVarSecretStore();
		assertArrayEquals(System.getenv(name.get()).toCharArray(), store.find(name.get()).orElseThrow());
	}

	@Test void a02_existsExistingEnvVar() {
		var name = anyEnvVar();
		assumeTrue(name.isPresent(), "No environment variables available");
		assertTrue(new EnvVarSecretStore().exists(name.get()));
	}

	@Test void a03_findAbsentEnvVarReturnsEmpty() {
		assertTrue(new EnvVarSecretStore().find("JUNEAU_DEFINITELY_NOT_SET_9f3a").isEmpty());
	}

	@Test void a04_existsAbsentEnvVarReturnsFalse() {
		assertFalse(new EnvVarSecretStore().exists("JUNEAU_DEFINITELY_NOT_SET_9f3a"));
	}

	@Test void a05_storeIsUnsupported() {
		var store = new EnvVarSecretStore();
		assertThrows(UnsupportedOperationException.class, () -> store.store("k", "v".toCharArray()));
	}

	@Test void a06_deleteIsUnsupported() {
		var store = new EnvVarSecretStore();
		assertThrows(UnsupportedOperationException.class, () -> store.delete("k"));
	}

	@Test void a07_nullKeyRejected() {
		var store = new EnvVarSecretStore();
		assertThrows(IllegalArgumentException.class, () -> store.find(null));
		assertThrows(IllegalArgumentException.class, () -> store.exists(null));
	}
}
