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

import org.apache.juneau.config.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;

/**
 * Guard that protects {@link LoadConfigResource} with a shared API key.
 *
 * <p>
 * The expected key is read from the <c>[Security]</c> section of <c>juneau-server-config.cfg</c>:
 * <p class='bini'>
 * 	<cs>[Security]</cs>
 * 	<ck>apiKey</ck> = <cv>some-long-random-secret</cv>
 * </p>
 *
 * <p>
 * Clients supply the key in either an <c>Authorization: Bearer &lt;key&gt;</c> header or an <c>X-Api-Key</c> header.
 * The comparison is constant-time ({@link MessageDigest#isEqual(byte[], byte[])} over UTF-8 bytes).
 *
 * <p>
 * The guard <b>fails closed</b>: if no key is configured (missing, empty or unreadable setting), every request is
 * rejected with a 403.  The key is never logged or echoed.  Always use TLS.
 */
public class ConfigApiKeyGuard extends RestGuard {

	private static final String CONFIG_NAME = "juneau-server-config.cfg";
	private static final String CONFIG_KEY = "Security/apiKey";
	private static final String BEARER = "Bearer ";

	@Override /* Overridden from RestGuard */
	public boolean guard(RestRequest req, RestResponse res) throws BasicHttpException {
		if (! isAuthorized(configuredKey(), req.getHeader("Authorization"), req.getHeader("X-Api-Key")))
			throw new Forbidden("Access denied.");
		return true;
	}

	@Override /* Overridden from RestGuard */
	public boolean isRequestAllowed(RestRequest req) {
		return isAuthorized(configuredKey(), req.getHeader("Authorization"), req.getHeader("X-Api-Key"));
	}

	/**
	 * Returns the configured key, or <jk>null</jk> if none is configured or the configuration cannot be read.
	 *
	 * @return The configured key, or <jk>null</jk>.
	 */
	protected String configuredKey() {
		try {
			return Config.create().name(CONFIG_NAME).build().get(CONFIG_KEY).orElse(null);
		} catch (@SuppressWarnings("unused") Exception e) {
			// Fail closed; deliberately do not log (avoid leaking config content).
			return null;
		}
	}

	static boolean isAuthorized(String configured, String authorization, String xApiKey) {
		if (configured == null || configured.isEmpty())
			return false;
		var expected = configured.getBytes(StandardCharsets.UTF_8);
		var ok = false;
		if (authorization != null && authorization.regionMatches(true, 0, BEARER, 0, BEARER.length()))
			ok = matches(expected, authorization.substring(BEARER.length()).trim());
		if (xApiKey != null)
			ok |= matches(expected, xApiKey.trim());
		return ok;
	}

	private static boolean matches(byte[] expected, String supplied) {
		return MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8));
	}
}
