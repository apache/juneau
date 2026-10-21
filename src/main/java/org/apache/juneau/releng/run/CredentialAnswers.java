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

import java.util.Map;
import java.util.Optional;

import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.AccountStore;
import org.apache.juneau.releng.credential.CredentialSpec;

/**
 * Stored answers for release.py's {@code credential.*} prompt keys. Values are read per lookup and never cached or
 * logged.
 */
public class CredentialAnswers {

	private final Map<CredentialSpec, SecretStore> stores;
	private final AccountStore accounts;
	private final String committerEmail;

	/**
	 * Constructor.
	 *
	 * @param stores One store per credential (keychain in production).
	 * @param accounts The persisted availid and GPG key id.
	 * @param committerEmail {@code rm.git.committer.email}.
	 */
	public CredentialAnswers(Map<CredentialSpec, SecretStore> stores, AccountStore accounts, String committerEmail) {
		this.stores = stores;
		this.accounts = accounts;
		this.committerEmail = committerEmail;
	}

	/**
	 * The stored value for a prompt key.
	 *
	 * @param key A {@code credential.*} key.
	 * @return The value, or empty when none is stored (the page then asks).
	 */
	public Optional<String> lookup(String key) {
		var v = switch (key) {
			case "credential.apache.availid" -> availid();
			case "credential.apache.password" -> secret(CredentialSpec.APACHE_LDAP, availid());
			case "credential.apache.email" -> committerEmail;
			case "credential.gpg.keyid" -> gpgKeyId();
			case "credential.gpg.passphrase" -> secret(CredentialSpec.GPG, gpgKeyId());
			case "credential.github.token" -> secret(CredentialSpec.GITHUB, "token");
			default -> null;
		};
		return v == null || v.isBlank() ? Optional.empty() : Optional.of(v);
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
