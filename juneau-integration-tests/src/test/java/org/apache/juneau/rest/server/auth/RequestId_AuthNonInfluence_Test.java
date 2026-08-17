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
package org.apache.juneau.rest.server.auth;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.security.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Proves the always-on {@code X-Request-Id} correlation resolver has <b>zero</b> influence on authentication.
 *
 * <p>
 * The resolver never feeds the auth decision: a specific authorizer ({@link ApiKeyGuard}) yields a byte-identical
 * outcome regardless of the correlation id, paired with the {@code auth/**} grep tripwire (zero hits for
 * {@code REQUEST_ID}/{@code getRequestId}/{@code X-Request-Id}).  The final case is the documented <b>anti-pattern</b>:
 * pointing an auth guard at {@code X-Request-Id} turns a client-supplied correlation id into the credential &mdash;
 * "this is how you shoot yourself in the foot", and is out-of-contract / unsupported.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource"  // MockRestClient instances are short-lived test fixtures.
})
class RequestId_AuthNonInfluence_Test extends TestBase {

	private static final Map<String,Principal> KEYS = Map.of("alice-key", () -> "alice");
	private static final ApiKeyStore STORE = key -> o(KEYS.get(key));

	//------------------------------------------------------------------------------------------------------------------
	// Correctly-configured guard (reads X-API-Key): the correlation id does not change the auth outcome.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).append(ApiKeyGuard.create().store(STORE).build()).build();
		}
		@RestGet(path="/me")
		public String me(@Auth Principal p) {
			return p == null ? "null" : p.getName();
		}
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_authOutcomeIdenticalRegardlessOfRequestId() throws Exception {
		// Same valid key, two very different X-Request-Id values → byte-identical auth result.
		var admin = CA.get("/me").header("X-API-Key", "alice-key").header("X-Request-Id", "admin-0000-0000-0000-000000000001")
			.run().assertStatus(200).getContent().asString();
		var random = CA.get("/me").header("X-API-Key", "alice-key").header("X-Request-Id", "zZ9-random-correlation-99")
			.run().assertStatus(200).getContent().asString();
		assertEquals("alice", admin);
		assertEquals(admin, random);
	}

	@Test void a02_requestIdCannotSubstituteForCredential() throws Exception {
		// A valid-looking X-Request-Id (and no or wrong API key) never authenticates.
		CA.get("/me").header("X-Request-Id", "alice-key").run().assertStatus(401);
		CA.get("/me").header("X-API-Key", "not-a-key").header("X-Request-Id", "alice-key").run().assertStatus(401);
	}

	@Test void a03_resolverStillRunsUnderAuth() throws Exception {
		// The resolver is independent of auth: it still echoes the (distinct) correlation ids on 200 responses.
		var id1 = CA.get("/me").header("X-API-Key", "alice-key").header("X-Request-Id", "corr-alpha")
			.run().assertStatus(200).getHeader("X-Request-Id").asString().orElseThrow();
		var id2 = CA.get("/me").header("X-API-Key", "alice-key").header("X-Request-Id", "corr-beta")
			.run().assertStatus(200).getHeader("X-Request-Id").asString().orElseThrow();
		assertEquals("corr-alpha", id1);
		assertEquals("corr-beta", id2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Anti-pattern: a guard misconfigured to read X-Request-Id makes the correlation id the credential (unsupported).
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean
		public RestGuardList guards(BeanStore bs) {
			// DO NOT DO THIS: pointing the auth guard at the correlation header turns a client-supplied id into a key.
			return RestGuardList.create(bs).append(ApiKeyGuard.create().store(STORE).fromHeader("X-Request-Id").build()).build();
		}
		@RestGet(path="/me")
		public String me(@Auth Principal p) {
			return p == null ? "null" : p.getName();
		}
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_footgun_requestIdBecomesCredential() throws Exception {
		// The anti-pattern: a client that sends X-Request-Id equal to a valid key authenticates.
		CB.get("/me").header("X-Request-Id", "alice-key").run().assertStatus(200).assertContent().asString().is("alice");
		// A random correlation id is rejected — demonstrating the correlation id is (wrongly) load-bearing here.
		CB.get("/me").header("X-Request-Id", "just-a-correlation-id").run().assertStatus(401);
	}
}
