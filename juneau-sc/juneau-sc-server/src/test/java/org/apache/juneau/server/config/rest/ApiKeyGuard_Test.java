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

@SuppressWarnings("javadoc")
class ApiKeyGuard_Test {

	@Test void a01_noKeyConfigured_rejectsEverything() {
		assertFalse(ApiKeyGuard.isAuthorized(null, "Bearer x", "x"));
		assertFalse(ApiKeyGuard.isAuthorized("", "Bearer ", ""));
		assertFalse(ApiKeyGuard.isAuthorized("", null, null));
	}

	@Test void a02_wrongKey() {
		assertFalse(ApiKeyGuard.isAuthorized("secret", "Bearer nope", null));
		assertFalse(ApiKeyGuard.isAuthorized("secret", null, "nope"));
		assertFalse(ApiKeyGuard.isAuthorized("secret", "Basic secret", null));
	}

	@Test void a03_bearer() {
		assertTrue(ApiKeyGuard.isAuthorized("secret", "Bearer secret", null));
	}

	@Test void a04_xApiKey() {
		assertTrue(ApiKeyGuard.isAuthorized("secret", null, "secret"));
	}

	@Test void a05_noHeaders() {
		assertFalse(ApiKeyGuard.isAuthorized("secret", null, null));
	}
}
