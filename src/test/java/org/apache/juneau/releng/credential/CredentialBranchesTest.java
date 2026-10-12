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
package org.apache.juneau.releng.credential;

import static org.junit.jupiter.api.Assertions.*;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import org.apache.juneau.commons.secret.InMemorySecretStore;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.Validator.ValidationResult;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.sun.net.httpserver.HttpServer;

/**
 * Branch and error-path coverage for the credential package.
 */
class CredentialBranchesTest {

	private static ProcessRunner runner(List<ProcessRunner.ProcResult> results) {
		return new GpgValidatorTest.RecordingRunner(results);
	}

	private static ProcessRunner.ProcResult pr(int code, String out) {
		return new ProcessRunner.ProcResult(code, out);
	}

	// ---- CredentialSpec / CredentialStatus ----

	@Test
	void a01_specLookup() {
		assertEquals(CredentialSpec.GPG, CredentialSpec.byName("GPG"));
		assertThrows(IllegalArgumentException.class, () -> CredentialSpec.byName("nope"));
		assertNull(CredentialSpec.GPG.fixedAccount());
		assertEquals("token", CredentialSpec.GITHUB.fixedAccount());
		assertNull(new CredentialStatus().name);
	}

	// ---- CredentialService ----

	private CredentialService svc(Path dir, SecretStore ldapStore) {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, spec == CredentialSpec.APACHE_LDAP ? ldapStore : new InMemorySecretStore());
		var validators = new EnumMap<CredentialSpec, Validator>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			validators.put(spec, (s, a) -> ValidationResult.ok("fine " + a));
		return new CredentialService(stores, validators, new AccountStore(dir));
	}

	@Test
	void a02_validateWhenAccountKnownButSecretMissing(@TempDir Path dir) {
		var svc = svc(dir, new InMemorySecretStore());
		new AccountStore(dir).put(CredentialSpec.APACHE_LDAP, "jb");
		var r = svc.validate("apache");
		assertFalse(r.valid());
		assertTrue(r.message().contains("not set"));
		assertFalse(svc.validate("gpg").valid(), "no GPG key id recorded");
	}

	@Test
	void a03_storeValidateDeleteRoundTrip(@TempDir Path dir) {
		var svc = svc(dir, new InMemorySecretStore());
		svc.storeWithAccount("apache", "jb", "pw");
		assertEquals("fine jb", svc.validate("apache").message());
		assertTrue(svc.status().stream().anyMatch(s -> s.name.equals("apache") && s.present && s.lastValid));
		svc.delete("apache");
		assertFalse(svc.status().stream().filter(s -> s.name.equals("apache")).findFirst().orElseThrow().present);
		// Deleting again: no account is known any more, so the secret store is not touched.
		svc.delete("apache");
		svc.delete("github");
	}

	// ---- AccountStore ----

	@Test
	void a04_accountStoreEdgeCases(@TempDir Path dir) {
		var store = new AccountStore(dir);
		store.remove(CredentialSpec.GPG); // nothing stored: no save
		assertFalse(Files.exists(dir.resolve("accounts.properties")));
		assertTrue(store.get(CredentialSpec.GPG).isEmpty());
		store.put(CredentialSpec.GPG, "KEY");
		assertEquals("KEY", store.get(CredentialSpec.GPG).orElseThrow());
		store.remove(CredentialSpec.GPG);
		assertTrue(store.get(CredentialSpec.GPG).isEmpty());
		store.put(CredentialSpec.GPG, "  ");
		assertTrue(store.get(CredentialSpec.GPG).isEmpty(), "a blank value counts as unset");
	}

	@Test
	void a05_accountStoreIoFailures(@TempDir Path dir) throws Exception {
		// accounts.properties is a directory: not a regular file, so load() starts empty, then save() cannot write.
		Files.createDirectories(dir.resolve("accounts.properties"));
		var store = new AccountStore(dir);
		assertTrue(store.get(CredentialSpec.GPG).isEmpty());
		assertThrows(RuntimeException.class, () -> store.put(CredentialSpec.GPG, "K"));

		var d2 = dir.resolve("d2");
		Files.createDirectories(d2);
		var f = Files.writeString(d2.resolve("accounts.properties"), "gpg=K\n");
		Files.setPosixFilePermissions(f, java.nio.file.attribute.PosixFilePermissions.fromString("---------"));
		var unreadable = new AccountStore(d2);
		try {
			assertThrows(RuntimeException.class, () -> unreadable.get(CredentialSpec.GPG));
		} finally {
			Files.setPosixFilePermissions(f, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
		}
	}

	// ---- GithubTokenValidator ----

	private static String gh(int code, String out) {
		return new GithubTokenValidator(runner(List.of(pr(code, out)))).validate("t", "token").message();
	}

	@Test
	void a06a_githubNotInstalled() {
		assertTrue(gh(127, "").contains("not installed"));
		assertTrue(gh(1, "bash: gh: No such file or directory").contains("not installed"));
		assertTrue(gh(1, "gh: command not found").contains("not installed"));
	}

	@Test
	void a06b_githubHttpStatusReasons() {
		assertTrue(gh(1, "HTTP 401: Bad credentials").contains("401"));
		assertTrue(gh(1, "HTTP 403").contains("403"));
		assertTrue(gh(1, "insufficient permissions").contains("403"));
		assertTrue(gh(1, "missing scope").contains("403"));
		assertTrue(gh(1, "HTTP 404").contains("404"));
	}

	@Test
	void a06c_githubUnreachable() {
		assertTrue(gh(1, "dial tcp: lookup").contains("unreachable"));
		assertTrue(gh(1, "could not resolve host").contains("unreachable"));
		assertTrue(gh(1, "connection refused").contains("unreachable"));
		assertTrue(gh(1, "i/o timeout").contains("unreachable"));
		assertTrue(gh(1, "network is unreachable").contains("unreachable"));
	}

	@Test
	void a06d_githubExitCodeAndSuccess() {
		assertTrue(gh(7, "weird").contains("exit code 7"));
		assertTrue(gh(7, null).contains("exit code 7"));
		assertEquals("GitHub token OK.", gh(0, null));
		assertEquals("GitHub token OK.", gh(0, "not a valid login!"));
		assertEquals("GitHub token OK (user jb-1).", gh(0, "jb-1\n"));
	}

	// ---- GpgValidator ----

	private static String gpgSign(int code, String out) {
		return new GpgValidator(runner(List.of(pr(0, "sec"), pr(code, out)))).validate("p", "K").message();
	}

	@Test
	void a07_gpgReasons() {
		assertTrue(gpgSign(2, "bad session key").contains("rejected"));
		assertTrue(gpgSign(2, "No secret key").contains("no secret key"));
		assertTrue(gpgSign(2, "no such key").contains("no secret key"));
		assertTrue(gpgSign(2, "Inappropriate ioctl").contains("pinentry"));
		assertTrue(gpgSign(2, null).contains("exit code 2"));
		assertTrue(gpgSign(1, "gpg: command not found").contains("not installed"));
		assertTrue(gpgSign(127, null).contains("not installed"));
		assertTrue(gpgSign(1, "No such file or directory").contains("not installed"));
		var noOut = new GpgValidator(runner(List.of(pr(2, null)))).validate("p", "K");
		assertEquals("No secret key for K.", noOut.message());
	}

	// ---- ApacheLdapValidator (real HTTP against an in-process server) ----

	private static ValidationResult nexus(int status, String[] seenAuth) throws Exception {
		var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/status", ex -> {
			seenAuth[0] = ex.getRequestHeaders().getFirst("Authorization");
			ex.sendResponseHeaders(status, -1);
			ex.close();
		});
		server.start();
		try {
			var url = "http://127.0.0.1:" + server.getAddress().getPort() + "/status";
			return new ApacheLdapValidator(HttpClient.newHttpClient(), url).validate("pw", "jb");
		} finally {
			server.stop(0);
		}
	}

	@Test
	void a08_ldapStatusCodes() throws Exception {
		var auth = new String[1];
		var ok = nexus(200, auth);
		assertTrue(ok.valid());
		assertEquals("Basic " + java.util.Base64.getEncoder().encodeToString("jb:pw".getBytes()), auth[0]);
		assertTrue(nexus(401, auth).message().contains("rejected"));
		assertTrue(nexus(403, auth).message().contains("rejected"));
		assertTrue(nexus(500, auth).message().contains("Unexpected Nexus status 500"));
	}

	@Test
	void a09_ldapTransportErrors() {
		// Nothing listens on port 1.
		var r = new ApacheLdapValidator(HttpClient.newHttpClient(), "http://127.0.0.1:1/status").validate("pw", "jb");
		assertFalse(r.valid());
		assertTrue(r.message().startsWith("Nexus check error"));
		var bad = new ApacheLdapValidator(HttpClient.newHttpClient(), "not a uri").validate("pw", "jb");
		assertTrue(bad.message().startsWith("Nexus check error"));
		assertNotNull(new ApacheLdapValidator());
		Thread.currentThread().interrupt();
		var intr = new ApacheLdapValidator(HttpClient.newHttpClient(), "http://127.0.0.1:1/status").validate("pw", "jb");
		assertTrue(Thread.interrupted(), "interrupt flag is re-asserted");
		assertFalse(intr.valid());
	}
}
