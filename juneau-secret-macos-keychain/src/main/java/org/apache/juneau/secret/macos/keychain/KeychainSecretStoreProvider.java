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
package org.apache.juneau.secret.macos.keychain;

import org.apache.juneau.commons.secret.*;

/**
 * Provider that contributes a {@link KeychainSecretStore} via {@code ServiceLoader}, discoverable through
 * {@link SecretStores#fromServiceLoader()} when this module is on the classpath and the host is macOS.
 *
 * <p>
 * The service name defaults to {@value #DEFAULT_SERVICE} but can be overridden via the
 * {@value #SERVICE_PROPERTY} system property; the fail mode defaults to {@link FailMode#FAIL_CLOSED}.  On a
 * non-macOS host (or when the keychain CLI is absent) {@link #create()} returns <jk>null</jk> so discovery skips it.
 *
 * @since 10.0.0
 */
public class KeychainSecretStoreProvider implements SecretStoreProvider {

	/** System property overriding the keychain service name used by the discovered store. */
	public static final String SERVICE_PROPERTY = "juneau.secret.macos.keychain.service";

	/** Default keychain service name when {@link #SERVICE_PROPERTY} is unset. */
	public static final String DEFAULT_SERVICE = "org.apache.juneau";

	@Override /* SecretStoreProvider */
	public SecretStore create() {
		if (! System.getProperty("os.name", "").toLowerCase().contains("mac"))
			return null;
		return new KeychainSecretStore(System.getProperty(SERVICE_PROPERTY, DEFAULT_SERVICE));
	}

	@Override /* SecretStoreProvider */
	public int order() {
		return 10;
	}
}
