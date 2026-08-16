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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.apache.juneau.commons.secret.SecretStore;
import org.apache.juneau.releng.credential.Validator.ValidationResult;

/**
 * Stores + validates the managed credentials. Accounts: GitHub uses the fixed account "token"; Apache uses
 * the availid; GPG uses the key ID. The last validation result per credential is cached in memory for the UI.
 *
 * <p>Each {@link CredentialSpec} is backed by its own {@link SecretStore} instance, namespaced by
 * {@link CredentialSpec#keychainService}; the resolved account (availid/keyId/fixed "token") is the store key,
 * preserving the keychain service+account coordinates the app has always used.
 *
 * <p>The non-fixed accounts (availid/keyId) are not secrets, so they're persisted via {@link AccountStore}
 * rather than the Keychain — this is what lets a lookup survive an app restart.
 */
public class CredentialService {

	private final Map<CredentialSpec, SecretStore> stores;
	private final Map<CredentialSpec, Validator> validators;
	private final Map<CredentialSpec, ValidationResult> lastResults = new EnumMap<>(CredentialSpec.class);
	private final AccountStore accounts;

	public CredentialService(Map<CredentialSpec, SecretStore> stores, Map<CredentialSpec, Validator> validators,
			AccountStore accounts) {
		this.stores = stores;
		this.validators = validators;
		this.accounts = accounts;
	}

	/** Store a credential whose account is fixed (GitHub) or already known. */
	public void store(String name, String account, String secret) {
		var spec = CredentialSpec.byName(name);
		var acct = spec.accountIsFixed() ? spec.fixedAccount() : account;
		var chars = secret.toCharArray();
		try {
			stores.get(spec).store(acct, chars);
		} finally {
			Arrays.fill(chars, '\0');
		}
		if (!spec.accountIsFixed())
			accounts.put(spec, acct);
		lastResults.remove(spec);
	}

	/** Store a credential supplying its account explicitly (Apache availid / GPG key ID). */
	public void storeWithAccount(String name, String account, String secret) {
		store(name, account, secret);
	}

	/** Run the live validation for one credential, caching + returning the result. */
	public ValidationResult validate(String name) {
		var spec = CredentialSpec.byName(name);
		var acct = resolveAccount(spec);
		if (acct == null) {
			var r = ValidationResult.fail(spec.label + " is not set");
			lastResults.put(spec, r);
			return r;
		}
		var found = stores.get(spec).find(acct);
		if (found.isEmpty()) {
			var r = ValidationResult.fail(spec.label + " is not set");
			lastResults.put(spec, r);
			return r;
		}
		var chars = found.get();
		try {
			var r = validators.get(spec).validate(new String(chars), acct);
			lastResults.put(spec, r);
			return r;
		} finally {
			Arrays.fill(chars, '\0');
		}
	}

	/** Delete a credential from the store. */
	public void delete(String name) {
		var spec = CredentialSpec.byName(name);
		var acct = resolveAccount(spec);
		if (acct != null)
			stores.get(spec).delete(acct);
		if (!spec.accountIsFixed())
			accounts.remove(spec);
		lastResults.remove(spec);
	}

	/** Presence + last-validation status for all credentials (never the secret). */
	public List<CredentialStatus> status() {
		var out = new ArrayList<CredentialStatus>();
		for (var spec : CredentialSpec.values()) {
			var acct = resolveAccount(spec);
			var present = acct != null && stores.get(spec).exists(acct);
			var cs = new CredentialStatus(spec.id, spec.label, present);
			var last = lastResults.get(spec);
			if (last != null) {
				cs.lastValid = last.valid();
				cs.lastMessage = last.message();
			}
			out.add(cs);
		}
		return out;
	}

	private String resolveAccount(CredentialSpec spec) {
		if (spec.accountIsFixed())
			return spec.fixedAccount();
		return accounts.get(spec).orElse(null); // persisted in rm.state.dir — survives restart
	}
}
