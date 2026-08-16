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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import org.apache.juneau.commons.secret.SecretStore;

/**
 * Persists the non-secret {@code CredentialSpec -> account} mapping (Apache availid, GPG key ID) to
 * {@code accounts.properties} under {@code rm.state.dir}, so lookups survive an app restart even though the
 * secrets themselves live only in the Keychain (via {@link SecretStore}).
 *
 * <p>An availid/key ID isn't a secret — it's a public identifier — so it belongs in a plain state file, not
 * the Keychain. GitHub isn't tracked here: its account is the fixed literal {@code "token"}
 * (see {@link CredentialSpec#accountIsFixed()}).
 */
public class AccountStore {

	private final Path file;

	public AccountStore(Path stateDir) {
		this.file = stateDir.resolve("accounts.properties");
	}

	/** The persisted account for {@code spec}, or empty if never stored (or since deleted). */
	public synchronized Optional<String> get(CredentialSpec spec) {
		return Optional.ofNullable(load().getProperty(spec.id)).filter(s -> !s.isBlank());
	}

	/** Persist {@code account} for {@code spec} immediately, creating {@code rm.state.dir} if absent. */
	public synchronized void put(CredentialSpec spec, String account) {
		var props = load();
		props.setProperty(spec.id, account);
		save(props);
	}

	/** Forget the persisted account for {@code spec} (credential delete). */
	public synchronized void remove(CredentialSpec spec) {
		var props = load();
		if (props.remove(spec.id) != null)
			save(props);
	}

	private Properties load() {
		var props = new Properties();
		if (Files.isRegularFile(file)) {
			try (InputStream in = Files.newInputStream(file)) {
				props.load(in);
			} catch (IOException e) {
				throw isex(e, "Unreadable accounts file: %s", file);
			}
		}
		return props;
	}

	private void save(Properties props) {
		try {
			Files.createDirectories(file.getParent());
			try (OutputStream out = Files.newOutputStream(file)) {
				props.store(out, "Juneau Release Manager - non-secret account identifiers (availid / GPG key ID)");
			}
		} catch (IOException e) {
			throw isex(e, "Cannot save accounts file: %s", file);
		}
	}
}
