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
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import org.apache.juneau.commons.secret.InMemorySecretStore;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.Validator.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CredentialServiceTest {

	/** One in-memory {@link SecretStore} per credential, mirroring the per-service {@code KeychainSecretStore} wiring. */
	private static Map<CredentialSpec, SecretStore> stores() {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var spec : CredentialSpec.values())
			stores.put(spec, new InMemorySecretStore());
		return stores;
	}

	private CredentialService svc(Map<CredentialSpec, SecretStore> stores, Validator apache, Validator gpg,
			Validator github, AccountStore accounts) {
		var validators = new EnumMap<CredentialSpec, Validator>(CredentialSpec.class);
		validators.put(CredentialSpec.APACHE_LDAP, apache);
		validators.put(CredentialSpec.GPG, gpg);
		validators.put(CredentialSpec.GITHUB, github);
		return new CredentialService(stores, validators, accounts);
	}

	private CredentialService svc(Map<CredentialSpec, SecretStore> stores, Validator apache, Validator gpg,
			Validator github, Path accountsDir) {
		return svc(stores, apache, gpg, github, new AccountStore(accountsDir));
	}

	@Test
	void storeThenStatusShowsPresentWithoutLeakingSecret(@TempDir Path dir) {
		var stores = stores();
		var service = svc(stores, (s, a) -> ValidationResult.ok("x"), (s, a) -> ValidationResult.ok("x"),
				(s, a) -> ValidationResult.ok("x"), dir);
		service.store("github", "token", "ghp_abc");

		var all = service.status();
		var gh = all.stream().filter(c -> c.name.equals("github")).findFirst().orElseThrow();
		assertTrue(gh.present);
		// CredentialStatus has no field that could carry the secret.
		assertArrayEquals("ghp_abc".toCharArray(), stores.get(CredentialSpec.GITHUB).find("token").orElseThrow());
	}

	@Test
	void validateReadsSecretAndRecordsResult(@TempDir Path dir) {
		var stores = stores();
		var service = svc(stores, (s, a) -> ValidationResult.fail("nope"), (s, a) -> ValidationResult.ok("x"),
				(secret, acct) -> secret.equals("good") ? ValidationResult.ok("ok") : ValidationResult.fail("bad"),
				dir);
		service.store("github", "token", "good");

		var res = service.validate("github");
		assertTrue(res.valid());

		var gh = service.status().stream().filter(c -> c.name.equals("github")).findFirst().orElseThrow();
		assertEquals(Boolean.TRUE, gh.lastValid);
	}

	@Test
	void validateMissingCredentialFails(@TempDir Path dir) {
		var service = svc(stores(), (s, a) -> ValidationResult.ok("x"), (s, a) -> ValidationResult.ok("x"),
				(s, a) -> ValidationResult.ok("x"), dir);
		var res = service.validate("apache");
		assertFalse(res.valid());
		assertTrue(res.message().toLowerCase().contains("not set"));
	}

	@Test
	void apacheStoresAvailidAsAccount(@TempDir Path dir) {
		var stores = stores();
		var service = svc(stores, (s, a) -> ValidationResult.ok("x"), (s, a) -> ValidationResult.ok("x"),
				(s, a) -> ValidationResult.ok("x"), dir);
		service.storeWithAccount("apache", "jbognar", "pw");
		assertTrue(stores.get(CredentialSpec.APACHE_LDAP).exists("jbognar"));
	}

	@Test
	void apacheAccountIsPersistedAndSurvivesRestart(@TempDir Path dir) {
		var stores = stores();
		var service = svc(stores, (s, a) -> ValidationResult.ok("x"), (s, a) -> ValidationResult.ok("x"),
				(s, a) -> ValidationResult.ok("x"), dir);
		service.storeWithAccount("apache", "jbognar", "pw");

		// Simulate a restart: a brand-new CredentialService, but over the SAME (still-populated) SecretStores
		// and a fresh AccountStore instance pointed at the same state dir.
		var restarted = svc(stores, (s, a) -> ValidationResult.ok("x"), (s, a) -> ValidationResult.ok("x"),
				(s, a) -> ValidationResult.ok("x"), dir);

		var apache = restarted.status().stream().filter(c -> c.name.equals("apache")).findFirst().orElseThrow();
		assertTrue(apache.present);

		var res = restarted.validate("apache");
		assertTrue(res.valid());
	}

	@Test
	void deleteForgetsThePersistedAccount(@TempDir Path dir) {
		var stores = stores();
		var service = svc(stores, (s, a) -> ValidationResult.ok("x"), (s, a) -> ValidationResult.ok("x"),
				(s, a) -> ValidationResult.ok("x"), dir);
		service.storeWithAccount("apache", "jbognar", "pw");
		service.delete("apache");

		var restarted = svc(stores, (s, a) -> ValidationResult.ok("x"), (s, a) -> ValidationResult.ok("x"),
				(s, a) -> ValidationResult.ok("x"), dir);
		var apache = restarted.status().stream().filter(c -> c.name.equals("apache")).findFirst().orElseThrow();
		assertFalse(apache.present);
	}
}
