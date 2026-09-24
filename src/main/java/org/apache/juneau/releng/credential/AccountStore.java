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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Properties;
import org.apache.juneau.commons.secret.SecretStore;

/**
 * Persists the non-secret {@code CredentialSpec -> account} mapping (Apache availid, GPG key ID) to
 * {@code accounts.properties} under {@code rm.state.dir}, so lookups survive an app restart even though the
 * secrets themselves live only in the Keychain (via {@link SecretStore}).
 *
 * <p>GitHub isn't tracked here: its account is the fixed literal {@code "token"}
 * (see {@link CredentialSpec#accountIsFixed()}).
 */
public class AccountStore {

	private final Path file;

	/**
	 * Backs the store with {@code accounts.properties} under {@code stateDir}.
	 */
	public AccountStore(Path stateDir) {
		this.file = stateDir.resolve("accounts.properties");
	}

	/**
	 * The persisted account for {@code spec}, or empty if never stored (or since deleted).
	 */
	public synchronized Optional<String> get(CredentialSpec spec) {
		return Optional.ofNullable(load().getProperty(spec.id)).filter(s -> !s.isBlank());
	}

	/**
	 * Persist {@code account} for {@code spec} immediately, creating {@code rm.state.dir} if absent.
	 */
	public synchronized void put(CredentialSpec spec, String account) {
		var props = load();
		props.setProperty(spec.id, account);
		save(props);
	}

	/**
	 * Forget the persisted account for {@code spec} (credential delete).
	 */
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
			restrictPermissions();
		} catch (IOException e) {
			throw isex(e, "Cannot save accounts file: %s", file);
		}
	}

	/**
	 * Narrows the accounts file and its directory to the owner.
	 *
	 * <p>Silently skipped where POSIX permissions do not apply.
	 */
	@SuppressWarnings({
		"resource" // FileSystems.getDefault() returns the JVM-wide default filesystem singleton; it must not be closed (its close() throws UnsupportedOperationException), so there is no resource to release.
	})
	private void restrictPermissions() {
		var dir = file.getParent();
		if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
			return;
		trySetPermissions(dir, "rwx------");
		trySetPermissions(file, "rw-------");
	}

	private static void trySetPermissions(Path path, String mode) {
		try {
			Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode));
		} catch (IOException | UnsupportedOperationException e) {
			// See restrictPermissions: best-effort by design.
		}
	}
}
