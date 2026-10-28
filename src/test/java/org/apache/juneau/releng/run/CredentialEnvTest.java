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
package org.apache.juneau.releng.run;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.CredentialSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CredentialEnvTest {

	private static final class MapStore implements SecretStore {
		final Map<String, char[]> data = new HashMap<>();

		@Override
		public void store(String key, char[] secret) {
			data.put(key, secret);
		}

		@Override
		public Optional<char[]> find(String key) {
			return Optional.ofNullable(data.get(key));
		}

		@Override
		public boolean exists(String key) {
			return data.containsKey(key);
		}

		@Override
		public boolean delete(String key) {
			return data.remove(key) != null;
		}
	}

	@AfterEach
	void clearProps() {
		System.clearProperty("rm.availid");
		System.clearProperty("rm.gpg.keyid");
	}

	private CredentialEnv env(Path dir) {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var s : CredentialSpec.values())
			stores.put(s, new MapStore());
		var accounts = new AccountStore(dir);
		accounts.put(CredentialSpec.APACHE_LDAP, "jamesbognar");
		accounts.put(CredentialSpec.GPG, "ABCD1234");
		stores.get(CredentialSpec.APACHE_LDAP).store("jamesbognar", "pw".toCharArray());
		stores.get(CredentialSpec.GPG).store("ABCD1234", "pp".toCharArray());
		stores.get(CredentialSpec.GITHUB).store("token", "gh".toCharArray());
		return new CredentialEnv(stores, accounts);
	}

	@Test
	void a01_storedSecretsMapToTheScriptsEnvironmentNames(@TempDir Path dir) {
		assertEquals(Map.of("JRM_APACHE_PASSWORD", "pw", "JRM_GPG_PASSPHRASE", "pp", "JRM_GITHUB_TOKEN", "gh"),
			env(dir).environment());
	}

	@Test
	void a02_onlyStoredSecretsAreIncluded(@TempDir Path dir) {
		var stores = new EnumMap<CredentialSpec, SecretStore>(CredentialSpec.class);
		for (var s : CredentialSpec.values())
			stores.put(s, new MapStore());
		var accounts = new AccountStore(dir);
		accounts.put(CredentialSpec.APACHE_LDAP, "jamesbognar");
		stores.get(CredentialSpec.APACHE_LDAP).store("jamesbognar", "pw".toCharArray());
		assertEquals(Map.of("JRM_APACHE_PASSWORD", "pw"), new CredentialEnv(stores, accounts).environment());
	}

	@Test
	void a03_systemPropertyAccountIsHonoured(@TempDir Path dir) {
		System.setProperty("rm.availid", "other");
		assertFalse(env(dir).environment().containsKey("JRM_APACHE_PASSWORD"), "no secret is stored for 'other'");
	}

	@Test
	void a04_nothingStoredIsAnEmptyMap(@TempDir Path dir) {
		assertEquals(Map.of(), new CredentialEnv(new EnumMap<>(CredentialSpec.class), new AccountStore(dir)).environment());
	}
}
