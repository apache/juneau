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

import static org.apache.juneau.commons.utils.Shorts.*;

/** The set of managed credentials and their Keychain coordinates. */
public enum CredentialSpec {

	APACHE_LDAP("apache", "juneau-rm.apache", "Apache LDAP (Nexus + dist SVN)"),
	GPG("gpg", "juneau-rm.gpg", "GPG signing key"), GITHUB("github", "juneau-rm.github", "GitHub token");

	public final String id; // URL-safe id used in REST paths
	public final String keychainService; // Keychain service prefix
	public final String label; // human label for the UI

	CredentialSpec(String id, String keychainService, String label) {
		this.id = id;
		this.keychainService = keychainService;
		this.label = label;
	}

	public static CredentialSpec byName(String name) {
		for (var c : values())
			if (c.id.equalsIgnoreCase(name))
				return c;
		throw iaex("Unknown credential: %s", name);
	}

	/** GitHub's account is the fixed literal "token"; the others store the account with the secret. */
	public boolean accountIsFixed() {
		return this == GITHUB;
	}

	public String fixedAccount() {
		return this == GITHUB ? "token" : null;
	}
}
