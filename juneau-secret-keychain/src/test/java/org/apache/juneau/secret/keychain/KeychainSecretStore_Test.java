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
package org.apache.juneau.secret.keychain;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.secret.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link KeychainSecretStore}.
 *
 * <p>
 * The round-trip tests are guarded by JUnit assumptions so they skip cleanly when not on macOS or when the
 * {@code security} CLI is unavailable.  The {@link FailMode} and validation tests use a deliberately bad binary path
 * so they run deterministically on any platform without touching a real keychain.
 */
class KeychainSecretStore_Test {

	private static boolean keychainAvailable() {
		return System.getProperty("os.name", "").toLowerCase().contains("mac") && new File("/usr/bin/security").canExecute();
	}

	private static String uniqueService() {
		return "org.apache.juneau.test." + UUID.randomUUID();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Round-trip against the real keychain (assumption-guarded).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_roundTrip() {
		assumeTrue(keychainAvailable(), "macOS keychain CLI not available");
		var service = uniqueService();
		var store = new KeychainSecretStore(service);
		try {
			assertFalse(store.exists("acct"));
			assertTrue(store.find("acct").isEmpty());

			store.store("acct", "hunter2".toCharArray());
			assertTrue(store.exists("acct"));
			assertArrayEquals("hunter2".toCharArray(), store.find("acct").orElseThrow());

			// Update-in-place.
			store.store("acct", "s3cr3t".toCharArray());
			assertArrayEquals("s3cr3t".toCharArray(), store.find("acct").orElseThrow());

			assertTrue(store.delete("acct"));
			assertFalse(store.exists("acct"));
			assertTrue(store.find("acct").isEmpty());
			assertFalse(store.delete("acct"));
		} finally {
			try {
				store.delete("acct");
			} catch (RuntimeException ignored) { /* best-effort cleanup */ }
		}
	}

	@Test void a02_absentKeyIsCleanlyAbsent() {
		assumeTrue(keychainAvailable(), "macOS keychain CLI not available");
		var store = new KeychainSecretStore(uniqueService());
		assertTrue(store.find("missing").isEmpty());
		assertFalse(store.exists("missing"));
		assertFalse(store.delete("missing"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// FailMode on an unavailable backend (deterministic via a bad binary path).
	// -----------------------------------------------------------------------------------------------------------------

	private static KeychainSecretStore unavailable(FailMode failMode) {
		return new KeychainSecretStore("svc", failMode, 5L, "/nonexistent/juneau-not-security");
	}

	@Test void b01_failOpenReadsDegradeToAbsent() {
		var store = unavailable(FailMode.FAIL_OPEN);
		assertTrue(store.find("k").isEmpty());
		assertFalse(store.exists("k"));
		assertFalse(store.delete("k"));
	}

	@Test void b02_failOpenStoreIsNoOp() {
		assertDoesNotThrow(() -> unavailable(FailMode.FAIL_OPEN).store("k", "v".toCharArray()));
	}

	@Test void b03_failClosedReadsThrow() {
		var store = unavailable(FailMode.FAIL_CLOSED);
		assertThrows(RuntimeException.class, () -> store.find("k"));
		assertThrows(RuntimeException.class, () -> store.exists("k"));
		assertThrows(RuntimeException.class, () -> store.delete("k"));
	}

	@Test void b04_failClosedStoreThrows() {
		assertThrows(RuntimeException.class, () -> unavailable(FailMode.FAIL_CLOSED).store("k", "v".toCharArray()));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Construction validation (platform-independent).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_nullOrBlankServiceRejected() {
		assertThrows(IllegalArgumentException.class, () -> new KeychainSecretStore(null));
		assertThrows(IllegalArgumentException.class, () -> new KeychainSecretStore("  "));
	}

	@Test void c02_nullFailModeRejected() {
		assertThrows(IllegalArgumentException.class, () -> new KeychainSecretStore("svc", null));
	}

	@Test void c03_nonPositiveTimeoutRejected() {
		assertThrows(IllegalArgumentException.class, () -> new KeychainSecretStore("svc", FailMode.FAIL_CLOSED, 0L));
	}

	@Test void c04_nullKeyRejected() {
		var store = unavailable(FailMode.FAIL_OPEN);
		assertThrows(IllegalArgumentException.class, () -> store.find(null));
		assertThrows(IllegalArgumentException.class, () -> store.exists(null));
		assertThrows(IllegalArgumentException.class, () -> store.delete(null));
		assertThrows(IllegalArgumentException.class, () -> store.store(null, "v".toCharArray()));
		assertThrows(IllegalArgumentException.class, () -> store.store("k", null));
	}
}
