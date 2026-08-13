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
 * Coverage for {@link InMemorySecretStore}: the three-state find/exists/absent model, replacement, deletion, null
 * rejection, and the defensive-copy guarantees that let callers zero their own arrays.
 */
class InMemorySecretStore_Test extends TestBase {

	@Test void a01_findAbsentReturnsEmpty() {
		var store = new InMemorySecretStore();
		assertTrue(store.find("nope").isEmpty());
	}

	@Test void a02_existsAbsentReturnsFalse() {
		var store = new InMemorySecretStore();
		assertFalse(store.exists("nope"));
	}

	@Test void a03_storeThenFindReturnsValue() {
		var store = new InMemorySecretStore();
		store.store("db.password", "hunter2".toCharArray());
		assertArrayEquals("hunter2".toCharArray(), store.find("db.password").orElseThrow());
	}

	@Test void a04_storeThenExistsReturnsTrue() {
		var store = new InMemorySecretStore();
		store.store("k", "v".toCharArray());
		assertTrue(store.exists("k"));
	}

	@Test void a05_storeReplacesExistingValue() {
		var store = new InMemorySecretStore();
		store.store("k", "old".toCharArray());
		store.store("k", "new".toCharArray());
		assertArrayEquals("new".toCharArray(), store.find("k").orElseThrow());
	}

	@Test void a06_deletePresentReturnsTrueAndRemoves() {
		var store = new InMemorySecretStore();
		store.store("k", "v".toCharArray());
		assertTrue(store.delete("k"));
		assertFalse(store.exists("k"));
		assertTrue(store.find("k").isEmpty());
	}

	@Test void a07_deleteAbsentReturnsFalse() {
		var store = new InMemorySecretStore();
		assertFalse(store.delete("nope"));
	}

	@Test void a08_storeCopiesInput_callerMayZeroWithoutAffectingStore() {
		// Defensive copy on store(): zeroing the caller's array must not corrupt the stored secret.
		var store = new InMemorySecretStore();
		var secret = "hunter2".toCharArray();
		store.store("k", secret);
		Arrays.fill(secret, '\0');
		assertArrayEquals("hunter2".toCharArray(), store.find("k").orElseThrow());
	}

	@Test void a09_findCopiesOutput_callerMayZeroWithoutAffectingStore() {
		// Defensive copy on find(): zeroing a returned array must not corrupt the stored secret for the next reader.
		var store = new InMemorySecretStore();
		store.store("k", "hunter2".toCharArray());
		var first = store.find("k").orElseThrow();
		Arrays.fill(first, '\0');
		assertArrayEquals("hunter2".toCharArray(), store.find("k").orElseThrow());
	}

	@Test void a10_nullArgumentsRejected() {
		var store = new InMemorySecretStore();
		assertThrows(IllegalArgumentException.class, () -> store.store(null, "v".toCharArray()));
		assertThrows(IllegalArgumentException.class, () -> store.store("k", null));
		assertThrows(IllegalArgumentException.class, () -> store.find(null));
		assertThrows(IllegalArgumentException.class, () -> store.exists(null));
		assertThrows(IllegalArgumentException.class, () -> store.delete(null));
	}
}
