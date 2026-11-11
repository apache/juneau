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
package org.apache.juneau.server.config.rest;

import java.nio.charset.*;
import java.security.*;
import java.util.function.*;

import org.apache.juneau.config.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.guard.*;

/**
 * Guard that requires a shared API key on every request to {@link LoadConfigResource}.
 *
 * <p>
 * The expected key is read from the <c>apiKey</c> entry in the <c>[Security]</c> section of
 * <c>juneau-server-config.cfg</c> (<c>Security/apiKey</c>).
 * The caller supplies it either as <c>Authorization: Bearer &lt;key&gt;</c> or as <c>X-Api-Key: &lt;key&gt;</c>.
 *
 * <p>
 * This guard <b>fails closed</b>: if no key is configured (missing or empty), every request is rejected with a 403.
 * Keys are compared in constant time and are never logged or echoed.
 * Always use TLS, since the key is a bearer credential.
 */
@SuppressWarnings("javadoc")
public class ApiKeyGuard extends RestGuard {

	/** Configuration entry holding the expected API key. */
	public static final String CONFIG_KEY = "Security/apiKey";

	private static final String BEARER = "bearer ";

	private final Supplier<String> keySupplier;

	/** Default constructor; reads the key from juneau-server-config.cfg on each request. */
	public ApiKeyGuard() {
		this(ApiKeyGuard::loadConfiguredKey);
	}

	ApiKeyGuard(Supplier<String> keySupplier) {
		this.keySupplier = keySupplier;
	}

	private static String loadConfiguredKey() {
		try {
			return Config.create().name("juneau-server-config.cfg").build().get(CONFIG_KEY).orElse(null);
		} catch (Exception e) {
			return null;  // Fail closed.
		}
	}

	@Override /* Overridden from RestGuard */
	public boolean isRequestAllowed(RestRequest req) {
		String configured;
		try {
			configured = keySupplier.get();
		} catch (RuntimeException e) {
			return false;
		}
		return isAuthorized(configured, req.getHeader("Authorization"), req.getHeader("X-Api-Key"));
	}

	/**
	 * Decides whether the supplied credentials match the configured key.
	 *
	 * @param configured The configured key.  Null or empty means reject everything.
	 * @param authorization The <c>Authorization</c> header value, or null.
	 * @param xApiKey The <c>X-Api-Key</c> header value, or null.
	 * @return <jk>true</jk> if either credential equals the configured key.
	 */
	static boolean isAuthorized(String configured, String authorization, String xApiKey) {
		if (configured == null || configured.isEmpty())
			return false;
		var expected = configured.getBytes(StandardCharsets.UTF_8);
		var ok = false;
		if (authorization != null && authorization.regionMatches(true, 0, BEARER, 0, BEARER.length()))
			ok |= matches(expected, authorization.substring(BEARER.length()).trim());
		if (xApiKey != null)
			ok |= matches(expected, xApiKey.trim());
		return ok;
	}

	private static boolean matches(byte[] expected, String supplied) {
		return MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8));
	}
}
