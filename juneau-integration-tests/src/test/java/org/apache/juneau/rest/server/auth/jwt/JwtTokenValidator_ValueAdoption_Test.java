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
package org.apache.juneau.rest.server.auth.jwt;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Acceptance tests for {@code @Value}-driven defaults on {@link JwtTokenValidator.Builder}.
 *
 * <p>
 * 3-test triad per migrated field per OQA #4 — system property set, unset (default), and {@code Settings.setGlobal}.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class JwtTokenValidator_ValueAdoption_Test {

	private static final String TTL_KEY = "juneau.jwt.jwksCacheTtl";
	private static final String EAGER_KEY = "juneau.jwt.jwksEagerRefreshOnKidMiss";
	private static final String COOLDOWN_KEY = "juneau.jwt.jwksEagerRefreshCooldown";

	@AfterEach
	void cleanup() {
		for (var k : new String[]{TTL_KEY, EAGER_KEY, COOLDOWN_KEY}) {
			Settings.get().unsetGlobal(k);
			System.clearProperty(k);
		}
	}

	// ---- jwksCacheTtl -----------------------------------------------------------------------

	@Test
	void a01_jwksCacheTtl_set() {
		System.setProperty(TTL_KEY, "PT10M");
		assertEquals(Duration.ofMinutes(10), JwtTokenValidator.create().jwksCacheTtl);
	}

	@Test
	void a02_jwksCacheTtl_unset() {
		assertEquals(Duration.ofMinutes(5), JwtTokenValidator.create().jwksCacheTtl);
	}

	@Test
	void a03_jwksCacheTtl_setGlobal() {
		Settings.get().setGlobal(TTL_KEY, "PT15M");
		assertEquals(Duration.ofMinutes(15), JwtTokenValidator.create().jwksCacheTtl);
	}

	// ---- jwksEagerRefreshOnKidMiss ----------------------------------------------------------

	@Test
	void b01_jwksEagerRefreshOnKidMiss_set() {
		System.setProperty(EAGER_KEY, "false");
		assertEquals(false, JwtTokenValidator.create().jwksEagerRefreshOnKidMiss);
	}

	@Test
	void b02_jwksEagerRefreshOnKidMiss_unset() {
		assertEquals(true, JwtTokenValidator.create().jwksEagerRefreshOnKidMiss);
	}

	@Test
	void b03_jwksEagerRefreshOnKidMiss_setGlobal() {
		Settings.get().setGlobal(EAGER_KEY, "false");
		assertEquals(false, JwtTokenValidator.create().jwksEagerRefreshOnKidMiss);
	}

	// ---- jwksEagerRefreshCooldown -----------------------------------------------------------

	@Test
	void c01_jwksEagerRefreshCooldown_set() {
		System.setProperty(COOLDOWN_KEY, "PT30S");
		assertEquals(Duration.ofSeconds(30), JwtTokenValidator.create().jwksEagerRefreshCooldown);
	}

	@Test
	void c02_jwksEagerRefreshCooldown_unset() {
		assertEquals(Duration.ofSeconds(10), JwtTokenValidator.create().jwksEagerRefreshCooldown);
	}

	@Test
	void c03_jwksEagerRefreshCooldown_setGlobal() {
		Settings.get().setGlobal(COOLDOWN_KEY, "PT20S");
		assertEquals(Duration.ofSeconds(20), JwtTokenValidator.create().jwksEagerRefreshCooldown);
	}
}
