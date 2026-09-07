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

import static java.nio.charset.StandardCharsets.*;
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
 * The round-trip tests run against an in-memory {@link FakeKeychain} (via {@link KeychainSecretStore}'s
 * package-private {@code CommandRunner} seam) rather than the real {@code security} CLI, so they exercise this
 * store's real argument construction and output parsing deterministically on every OS &mdash; no
 * {@code /usr/bin/security}, no login-keychain mutation, and no GUI unlock/auth prompt.  The {@link FailMode} and
 * validation tests use a deliberately bad binary path so they also run deterministically on any platform without
 * touching a real keychain.  There is intentionally no test against the real {@code security} binary: doing so
 * without risking a hung, unattended GUI prompt on an interactive/locked login keychain would require this store to
 * support targeting an isolated, freshly-created keychain, which is out of scope here.
 */
class KeychainSecretStore_Test {

	private static boolean posixShellAvailable() {
		return new File("/bin/sh").canExecute();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Round-trip against an in-memory fake keychain (deterministic on every OS; never touches a real keychain).
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * An in-memory fake of the real {@code security} CLI, keyed by service+account, that models the
	 * {@code add-generic-password}/{@code find-generic-password}/{@code delete-generic-password} argument shapes
	 * and exit-code/stdout contract that {@link KeychainSecretStore} depends on &mdash; including {@code -U}
	 * update-in-place and the doubled value/confirmation {@code -w} stdin payload &mdash; so tests can exercise the
	 * store's real argument-construction and output-parsing logic without shelling out to a real keychain.
	 */
	private static final class FakeKeychain implements KeychainSecretStore.CommandRunner {

		private final Map<String,char[]> entries = new HashMap<>();

		@Override
		public KeychainSecretStore.Result run(byte[] stdin, String[] args) {
			return switch (args[0]) {
				case "add-generic-password" -> addGenericPassword(stdin, args);
				case "find-generic-password" -> findGenericPassword(args);
				case "delete-generic-password" -> deleteGenericPassword(args);
				default -> throw new IllegalArgumentException("Unsupported command: " + args[0]);
			};
		}

		private KeychainSecretStore.Result addGenericPassword(byte[] stdin, String[] args) {
			// The value and its CLI confirmation re-entry arrive doubled and newline-delimited (see
			// KeychainSecretStore.store's javadoc); both copies must match, mirroring the real prompt.
			var lines = new String(stdin, UTF_8).split("\n", -1);
			var value = lines[0];
			var confirmation = lines[1];
			if (! value.equals(confirmation))
				return new KeychainSecretStore.Result(1, new byte[0], "Password confirmation did not match.");
			entries.put(key(args), value.toCharArray());
			return new KeychainSecretStore.Result(0, new byte[0], "");
		}

		private KeychainSecretStore.Result findGenericPassword(String[] args) {
			var value = entries.get(key(args));
			if (value == null)
				return notFound();
			if (! Arrays.asList(args).contains("-w"))
				return new KeychainSecretStore.Result(0, new byte[0], "");
			return new KeychainSecretStore.Result(0, (new String(value) + "\n").getBytes(UTF_8), "");
		}

		private KeychainSecretStore.Result deleteGenericPassword(String[] args) {
			if (entries.remove(key(args)) == null)
				return notFound();
			return new KeychainSecretStore.Result(0, new byte[0], "");
		}

		private static KeychainSecretStore.Result notFound() {
			return new KeychainSecretStore.Result(KeychainSecretStore.NOT_FOUND, new byte[0], "The specified item could not be found in the keychain.");
		}

		private static String key(String[] args) {
			return opt(args, "-s") + "\u0000" + opt(args, "-a");
		}

		private static String opt(String[] args, String flag) {
			for (var i = 0; i < args.length - 1; i++)
				if (args[i].equals(flag))
					return args[i + 1];
			return null;
		}
	}

	@Test void a01_roundTrip() {
		var store = new KeychainSecretStore("svc", FailMode.FAIL_CLOSED, new FakeKeychain());

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
	}

	@Test void a02_absentKeyIsCleanlyAbsent() {
		var store = new KeychainSecretStore("svc", FailMode.FAIL_CLOSED, new FakeKeychain());
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
