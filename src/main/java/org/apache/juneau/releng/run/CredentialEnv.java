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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.CredentialSpec;

/**
 * The secrets JRM has stored, as the environment variables {@code release.py} reads ({@code JRM_APACHE_PASSWORD},
 * {@code JRM_GPG_PASSPHRASE}, {@code JRM_GITHUB_TOKEN}). Values are read per call and never cached or logged. Only
 * stored secrets are included; the Apache id, email and GPG key id come from the run's params file.
 */
public class CredentialEnv {

	private final Map<CredentialSpec, SecretStore> stores;
	private final AccountStore accounts;

	/**
	 * Constructor.
	 *
	 * @param stores One store per credential (keychain in production).
	 * @param accounts The persisted availid and GPG key id.
	 */
	public CredentialEnv(Map<CredentialSpec, SecretStore> stores, AccountStore accounts) {
		this.stores = stores;
		this.accounts = accounts;
	}

	/**
	 * The environment to add to a release.py child.
	 *
	 * @return {@code SECRET_ENV name -> value} for each stored secret; empty when none is stored.
	 */
	public Map<String, String> environment() {
		var m = new LinkedHashMap<String, String>();
		put(m, "JRM_APACHE_PASSWORD", secret(CredentialSpec.APACHE_LDAP, availid()));
		put(m, "JRM_GPG_PASSPHRASE", secret(CredentialSpec.GPG, gpgKeyId()));
		put(m, "JRM_GITHUB_TOKEN", secret(CredentialSpec.GITHUB, "token"));
		return m;
	}

	private static void put(Map<String, String> m, String name, String value) {
		if (value != null && ! value.isBlank())
			m.put(name, value);
	}

	// Precedence as the old secretResolver had it: the system property (headless/testing override), else the
	// AccountStore value.
	private String availid() {
		var sys = System.getProperty("rm.availid", "");
		return ! sys.isBlank() ? sys : accounts.get(CredentialSpec.APACHE_LDAP).orElse("");
	}

	private String gpgKeyId() {
		var sys = System.getProperty("rm.gpg.keyid", "");
		return ! sys.isBlank() ? sys : accounts.get(CredentialSpec.GPG).orElse("");
	}

	private String secret(CredentialSpec spec, String account) {
		var store = stores.get(spec);
		return store == null || account.isBlank() ? null : store.find(account).map(String::new).orElse(null);
	}
}
