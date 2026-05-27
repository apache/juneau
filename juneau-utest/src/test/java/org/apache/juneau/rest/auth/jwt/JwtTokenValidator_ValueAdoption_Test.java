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
package org.apache.juneau.rest.auth.jwt;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * TODO-92 acceptance tests for {@code @Value}-driven defaults on {@link JwtTokenValidator.Builder}.
 *
 * <p>
 * 3-test triad per migrated field per OQA #4 — system property set, unset (default), and {@code Settings.setGlobal}.
 */
class JwtTokenValidator_ValueAdoption_Test {

	private static final String SP = "juneau.jwt.jwksCacheTtl";

	@AfterEach
	void cleanup() {
		Settings.get().unsetGlobal(SP);
		System.clearProperty(SP);
	}

	@Test
	void a01_jwksCacheTtl_set() {
		System.setProperty(SP, "PT10M");
		assertEquals(Duration.ofMinutes(10), JwtTokenValidator.create().jwksCacheTtl);
	}

	@Test
	void a02_jwksCacheTtl_unset() {
		assertEquals(Duration.ofMinutes(5), JwtTokenValidator.create().jwksCacheTtl);
	}

	@Test
	void a03_jwksCacheTtl_setGlobal() {
		Settings.get().setGlobal(SP, "PT15M");
		assertEquals(Duration.ofMinutes(15), JwtTokenValidator.create().jwksCacheTtl);
	}
}
