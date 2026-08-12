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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link SessionStore}'s default methods, exercised via a minimal implementation that overrides
 * only the mandatory methods (so {@link SessionStore#supportsServerSideRevocation()},
 * {@link SessionStore#invalidateBySubject(String)}, and {@link SessionStore#invalidateBySessionId(String)}
 * run their default implementations).
 *
 * @since 10.0.0
 */
class SessionStore_Test extends TestBase {

	/** Minimal SessionStore implementation that relies entirely on the interface's default methods. */
	private static final class A01_MinimalSessionStore implements SessionStore {
		@Override
		public String createSessionCookieValue(OidcSession session) {
			return "cookie";
		}

		@Override
		public Optional<OidcSession> lookup(String cookieValue) {
			return Optional.empty();
		}

		@Override
		public void invalidate(String cookieValue) {
			// no-op
		}
	}

	@Test void a01_defaultMethods_notServerSideRevocable() {
		var store = new A01_MinimalSessionStore();
		assertFalse(store.supportsServerSideRevocation());
		assertThrows(UnsupportedOperationException.class, () -> store.invalidateBySubject("alice"));
		assertThrows(UnsupportedOperationException.class, () -> store.invalidateBySessionId("sess-1"));
	}
}
