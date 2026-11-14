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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link ConfigApiKeyGuard} authorization logic.
 */
class ConfigApiKeyGuard_Test {

	private static final String KEY = "s3cret-key";

	@Test void a01_noKeyConfigured_rejectsEverything() {
		assertFalse(ConfigApiKeyGuard.isAuthorized(null, "Bearer " + KEY, KEY));
		assertFalse(ConfigApiKeyGuard.isAuthorized("", "Bearer ", ""));
		assertFalse(ConfigApiKeyGuard.isAuthorized("", null, null));
	}

	@Test void a02_wrongKey_rejected() {
		assertFalse(ConfigApiKeyGuard.isAuthorized(KEY, "Bearer nope", null));
		assertFalse(ConfigApiKeyGuard.isAuthorized(KEY, null, "nope"));
		assertFalse(ConfigApiKeyGuard.isAuthorized(KEY, "Bearer " + KEY + "x", null));
	}

	@Test void a03_bearer_accepted() {
		assertTrue(ConfigApiKeyGuard.isAuthorized(KEY, "Bearer " + KEY, null));
	}

	@Test void a04_xApiKey_accepted() {
		assertTrue(ConfigApiKeyGuard.isAuthorized(KEY, null, KEY));
	}

	@Test void a05_noHeaders_rejected() {
		assertFalse(ConfigApiKeyGuard.isAuthorized(KEY, null, null));
		assertFalse(ConfigApiKeyGuard.isAuthorized(KEY, "Basic " + KEY, null));
	}

	@Test void a06_missingConfig_failsClosed() {
		var g = new ConfigApiKeyGuard();
		// juneau-server-config.cfg in the test classpath/working dir sets no Security/apiKey.
		assertNull(g.configuredKey());
	}

	@Test void b01_secureChannel_accepted() {
		assertTrue(ConfigApiKeyGuard.isChannelAcceptable(true, "10.1.2.3", false));
	}

	@Test void b02_insecureLoopback_accepted() {
		assertTrue(ConfigApiKeyGuard.isChannelAcceptable(false, "127.0.0.1", false));
		assertTrue(ConfigApiKeyGuard.isChannelAcceptable(false, "::1", false));
	}

	@Test void b03_insecureRemote_refused() {
		assertFalse(ConfigApiKeyGuard.isChannelAcceptable(false, "10.1.2.3", false));
		assertFalse(ConfigApiKeyGuard.isChannelAcceptable(false, null, false));
	}

	@Test void b04_insecureRemote_acceptedWhenAllowInsecure() {
		assertTrue(ConfigApiKeyGuard.isChannelAcceptable(false, "10.1.2.3", true));
	}
}
