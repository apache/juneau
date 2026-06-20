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
package org.apache.juneau.rest.server.auth.oidc.rp;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link OidcSession} &mdash; compact-constructor edge cases.
 *
 * @since 10.0.0
 */
class OidcSession_Test extends TestBase {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	/** Null roles in compact constructor — line 76 true branch → replaced with Collections.emptySet(). */
	@Test void a01_nullRoles_replacedWithEmptySet() {
		var session = new OidcSession(
			"id-1",
			"alice",
			Optional.empty(),
			new ClaimsPrincipal("alice", Map.of("sub", "alice")),
			null,  // null roles → defensive null-handling in compact constructor
			Optional.empty(),
			NOW,
			NOW.plus(Duration.ofHours(8)));
		assertNotNull(session.roles());
		assertTrue(session.roles().isEmpty());
	}
}
