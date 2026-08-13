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
package org.apache.juneau.commons.secret;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

/**
 * A <b>read-only</b> {@link SecretStore} backed by {@link System#getenv(String)}.
 *
 * <p>
 * Gives 12-factor deployments a zero-dependency way to feed secrets in as environment variables: {@link #find} and
 * {@link #exists} resolve straight from the process environment.  Because the environment is immutable from within the
 * process, the mutating operations are unsupported:
 * <ul>
 * 	<li>{@link #store(String, char[])} throws {@link UnsupportedOperationException}.
 * 	<li>{@link #delete(String)} throws {@link UnsupportedOperationException}.
 * </ul>
 *
 * <p>
 * Note that environment variables are inherently more exposed than a dedicated secret backend (they are visible to
 * child processes and to anything that can read the process environment), so this store trades secrecy strength for
 * deployment simplicity.  The retrieved value is never {@code toString()}'d, logged, or dumped by this class.
 *
 * @since 10.0.0
 */
public class EnvVarSecretStore implements SecretStore {

	@Override /* SecretStore */
	public void store(String key, char[] secret) {
		throw uoex("EnvVarSecretStore is read-only; environment variables cannot be modified from within the process.");
	}

	@Override /* SecretStore */
	public Optional<char[]> find(String key) {
		assertArgNotNull("key", key);
		var v = System.getenv(key);
		return v == null ? oe() : o(v.toCharArray());
	}

	@Override /* SecretStore */
	public boolean exists(String key) {
		assertArgNotNull("key", key);
		return System.getenv(key) != null;
	}

	@Override /* SecretStore */
	public boolean delete(String key) {
		throw uoex("EnvVarSecretStore is read-only; environment variables cannot be modified from within the process.");
	}
}
