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
import java.util.concurrent.*;

/**
 * Built-in default {@link SecretStore}: a per-process, {@link ConcurrentHashMap}-backed key-to-secret map.
 *
 * <p>
 * This is the zero-config store {@link SecretStores#resolve(org.apache.juneau.commons.inject.BeanStore)} falls back
 * to when no store has been contributed.  It is honest about its limits:
 *
 * <p>
 * <b>Process-local only.</b> Secrets live in this JVM's heap and are gone when the process exits &mdash; this store is
 * <b>not</b> persistent.  Two separate JVMs (or two instances behind a load balancer) each hold their own map and do
 * <b>not</b> see each other's secrets, so it is <b>not</b> cross-node.  It is a convenience default and a test double,
 * not a production secret backend; deployments that need durability or sharing must contribute a store backed by an
 * external secret manager.
 *
 * <p>
 * <b>Defensive copies.</b> {@link #store} copies the supplied array and {@link #find} returns a fresh copy, so the
 * caller may zero its own arrays without disturbing the stored value and vice-versa.  {@link #delete} zeroes the
 * retained array before dropping it.  The value is never {@code toString()}'d, logged, or dumped.
 *
 * <p>
 * <b>Thread-safety.</b> All operations are safe for concurrent invocation.
 *
 * @since 10.0.0
 */
public class InMemorySecretStore implements SecretStore {

	private final ConcurrentHashMap<String,char[]> secrets = new ConcurrentHashMap<>();

	@Override /* SecretStore */
	public void store(String key, char[] secret) {
		assertArgNotNull("key", key);
		assertArgNotNull("secret", secret);
		secrets.put(key, secret.clone());
	}

	@Override /* SecretStore */
	public Optional<char[]> find(String key) {
		assertArgNotNull("key", key);
		var v = secrets.get(key);
		return v == null ? oe() : o(v.clone());
	}

	@Override /* SecretStore */
	public boolean exists(String key) {
		assertArgNotNull("key", key);
		return secrets.containsKey(key);
	}

	@Override /* SecretStore */
	public boolean delete(String key) {
		assertArgNotNull("key", key);
		var v = secrets.remove(key);
		if (v == null)
			return false;
		Arrays.fill(v, '\0');
		return true;
	}
}
