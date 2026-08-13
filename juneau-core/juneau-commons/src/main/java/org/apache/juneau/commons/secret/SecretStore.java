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

import java.util.*;

/**
 * SPI for storing and retrieving a secret by key &mdash; the secure, mutable sibling of
 * {@link org.apache.juneau.commons.settings.PropertySource}.
 *
 * <p>
 * Where a {@code PropertySource} is a read-only source of ordinary configuration values, a {@code SecretStore}
 * adds <i>write</i>/<i>delete</i> plus <i>sensitivity</i> semantics.  The SPI is deliberately narrow &mdash; it
 * deals only in a {@link String} key and a {@code char[]} secret, with no dependency on any particular backend.
 *
 * <p>
 * <b>Sensitivity is intrinsic.</b> Secret values are held as {@code char[]} rather than {@link String} so they can
 * be explicitly zeroed and do not linger in the string pool.  An implementation must never {@code toString()}, log,
 * or otherwise dump a secret value.  Callers are responsible for zeroing the arrays they pass to {@link #store}
 * and receive from {@link #find} once done with them.
 *
 * <p>
 * <b>Three-state presence model.</b> The three operations together model a clean present-with-value /
 * present-without-materializing / absent distinction:
 * <ul>
 * 	<li>{@link #find(String)} &mdash; returns the secret value when present ({@link Optional} of {@code char[]}),
 * 		or {@link Optional#empty()} when absent.  This is the only method that materializes the value.
 * 	<li>{@link #exists(String)} &mdash; reports whether a secret is stored under the key <i>without</i> retrieving
 * 		(and, for backends that decrypt on read, without decrypting) it.  Prefer this over {@link #find} when only
 * 		presence matters, so the secret is never pulled into memory needlessly.
 * 	<li><i>absent</i> &mdash; {@link #find} returns empty and {@link #exists} returns <jk>false</jk>.
 * </ul>
 *
 * <p>
 * <b>Backend-unavailable behavior is a consumer decision.</b> The built-in {@link InMemorySecretStore} and
 * {@link EnvVarSecretStore} never fail on a well-formed call, so they simply throw on a genuine error.  Network- or
 * OS-backed implementations (for example an OS-keychain-backed store in a separate module) should let the consumer
 * pick fail-open vs fail-closed via a {@link FailMode} rather than baking a policy into the SPI.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link InMemorySecretStore}
 * 	<li class='jc'>{@link EnvVarSecretStore}
 * 	<li class='jc'>{@link FailMode}
 * 	<li class='jc'>{@link SecretStores}
 * </ul>
 *
 * @since 10.0.0
 */
public interface SecretStore {

	/**
	 * Stores a secret under the specified key, replacing any existing value.
	 *
	 * <p>
	 * The caller retains ownership of <jv>secret</jv> and may zero it after this call returns; an implementation
	 * that needs to retain the value must copy it.
	 *
	 * @param key The key under which to store the secret.  Must not be <jk>null</jk>.
	 * @param secret The secret value.  Must not be <jk>null</jk>.
	 * @throws UnsupportedOperationException If this store is read-only.
	 */
	void store(String key, char[] secret);

	/**
	 * Returns the secret stored under the specified key.
	 *
	 * <p>
	 * The returned array is the caller's to own and zero; it is not a live view of the store's internal state.
	 *
	 * @param key The key to look up.  Must not be <jk>null</jk>.
	 * @return The secret value, or {@link Optional#empty()} if no secret is stored under the key.
	 */
	Optional<char[]> find(String key);

	/**
	 * Returns whether a secret is stored under the specified key without materializing its value.
	 *
	 * @param key The key to check.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if a secret is stored under the key.
	 */
	boolean exists(String key);

	/**
	 * Deletes the secret stored under the specified key.
	 *
	 * @param key The key to delete.  Must not be <jk>null</jk>.
	 * @return <jk>true</jk> if a secret was present and removed; <jk>false</jk> if the key was already absent.
	 * @throws UnsupportedOperationException If this store is read-only.
	 */
	boolean delete(String key);
}
