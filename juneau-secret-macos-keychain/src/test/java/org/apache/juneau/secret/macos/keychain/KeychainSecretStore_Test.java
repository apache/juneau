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

import java.io.*;
import java.nio.file.*;
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

	private static boolean posixShellAvailable() {
		return new File("/bin/sh").canExecute();
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
	// Secret hygiene: the secret must never appear on the child process's argv.
	//
	// Uses a fake "security" shell script (in place of the real binary, via the package-private test constructor)
	// that dumps its own argv and its stdin to separate files, so the assertion does not depend on being able to
	// inspect a live process's command line (fragile/racy) or on running against the real keychain.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a03_secretNeverAppearsOnArgv() throws IOException {
		assumeTrue(posixShellAvailable(), "POSIX shell not available");
		var argvDump = File.createTempFile("juneau-keychain-argv", ".txt");
		var stdinDump = File.createTempFile("juneau-keychain-stdin", ".txt");
		var script = File.createTempFile("juneau-keychain-fake-security", ".sh");
		argvDump.deleteOnExit();
		stdinDump.deleteOnExit();
		script.deleteOnExit();
		try {
			Files.writeString(script.toPath(), ""
				+ "#!/bin/sh\n"
				+ "printf '%s\\n' \"$@\" > '" + argvDump.getAbsolutePath() + "'\n"
				+ "cat > '" + stdinDump.getAbsolutePath() + "'\n"
				+ "exit 0\n");
			assertTrue(script.setExecutable(true));

			var secret = "arg\u00eev-h0stile s3cr3t \t \u00fc";
			var store = new KeychainSecretStore("svc", FailMode.FAIL_CLOSED, 5L, script.getAbsolutePath());
			store.store("acct", secret.toCharArray());

			var argvContent = Files.readString(argvDump.toPath());
			assertFalse(argvContent.contains(secret), "secret must not appear in the child process's argv");

			var stdinContent = Files.readString(stdinDump.toPath());
			assertEquals(secret + "\n" + secret + "\n", stdinContent, "secret should be delivered via stdin, doubled for the CLI's confirmation prompt");
		} finally {
			argvDump.delete();
			stdinDump.delete();
			script.delete();
		}
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
		var store = unavailable(FailMode.FAIL_OPEN);
		var secret = "v".toCharArray();
		assertDoesNotThrow(() -> store.store("k", secret));
	}

	@Test void b03_failClosedReadsThrow() {
		var store = unavailable(FailMode.FAIL_CLOSED);
		assertThrows(RuntimeException.class, () -> store.find("k"));
		assertThrows(RuntimeException.class, () -> store.exists("k"));
		assertThrows(RuntimeException.class, () -> store.delete("k"));
	}

	@Test void b04_failClosedStoreThrows() {
		var store = unavailable(FailMode.FAIL_CLOSED);
		var secret = "v".toCharArray();
		assertThrows(RuntimeException.class, () -> store.store("k", secret));
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
		var secret = "v".toCharArray();
		assertThrows(IllegalArgumentException.class, () -> store.find(null));
		assertThrows(IllegalArgumentException.class, () -> store.exists(null));
		assertThrows(IllegalArgumentException.class, () -> store.delete(null));
		assertThrows(IllegalArgumentException.class, () -> store.store(null, secret));
		assertThrows(IllegalArgumentException.class, () -> store.store("k", null));
	}

	@Test void c05_secretWithNewlineOrCarriageReturnRejectedBeforeAnyProcessStarts() {
		// The stdin-delivered value/confirmation pair (see a03) is newline-delimited, so an embedded '\n' or '\r'
		// in the secret would desynchronize the CLI's confirmation prompt; reject it up front instead.
		var store = unavailable(FailMode.FAIL_CLOSED);
		var withNewline = "line1\nline2".toCharArray();
		var withCarriageReturn = "line1\rline2".toCharArray();
		assertThrows(IllegalArgumentException.class, () -> store.store("acct", withNewline));
		assertThrows(IllegalArgumentException.class, () -> store.store("acct", withCarriageReturn));
	}
}
