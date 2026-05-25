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
package org.apache.juneau.rest.ops;

import java.security.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.auth.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end integration smoke test for FINISHED-77's
 * {@link BasicAdminResource}-with-DenyAllGuard-default ↔ TODO-69's {@link BearerTokenGuard}
 * cross-cut. Confirms the migration claim from FINISHED-77's Related-work cross-link: swapping
 * {@link org.apache.juneau.rest.guard.DenyAllGuard DenyAllGuard} for an AuthN guard is a
 * zero-mixin-source-change drop-in.
 *
 * <p>
 * Each case sits on a host servlet that mounts {@link BasicAdminResource} as a mixin and supplies
 * its own {@code @Bean RestGuardList} containing a {@link BearerTokenGuard} backed by a
 * fixed-token validator. The framework's {@code @Bean RestGuardList} override REPLACES the
 * annotation-derived deny-all on the mixin, so the admin paths gate on the bearer token instead.
 *
 * @since 9.5.0
 */
class BasicAdminResource_AuthIntegration_Test extends TestBase {

	private static final Principal ADMIN = () -> "admin";

	/** Accepts only "secret-token"; throws for anything else. */
	private static final TokenValidator FIXED_TOKEN = token -> {
		if ("secret-token".equals(token))
			return ADMIN;
		throw new AuthenticationException("Invalid token");
	};

	@Rest(mixins=BasicAdminResource.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean
		public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs)
				.append(BearerTokenGuard.create().realm("admin").validator(FIXED_TOKEN).build())
				.build();
		}

		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	// -----------------------------------------------------------------------------------------
	// Missing token: 401 with bearer challenge - proves the AuthN guard is now gating /admin/*
	// instead of the DenyAllGuard which would have returned 403.
	// -----------------------------------------------------------------------------------------

	@Test void a01_missingTokenReturns401OnAdminThreads() throws Exception {
		ca.get("/admin/threads")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"admin\"");
	}

	@Test void a02_missingTokenReturns401OnAdminHeap() throws Exception {
		ca.get("/admin/heap")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"admin\"");
	}

	@Test void a03_missingTokenReturns401OnCacheFlush() throws Exception {
		ca.post("/admin/cache/flush", "")
			.run()
			.assertStatus(401);
	}

	// -----------------------------------------------------------------------------------------
	// Invalid token: validator throws -> 401, NOT 403. Confirms the swap.
	// -----------------------------------------------------------------------------------------

	@Test void b01_invalidTokenReturns401() throws Exception {
		ca.get("/admin/threads").header("Authorization", "Bearer wrong-token")
			.run()
			.assertStatus(401)
			.assertHeader("WWW-Authenticate").is("Bearer realm=\"admin\"")
			.assertContent().asString().isContains("Invalid token");
	}

	// -----------------------------------------------------------------------------------------
	// Valid token: admin paths serve.
	// -----------------------------------------------------------------------------------------

	@Test void c01_validTokenServesThreads() throws Exception {
		ca.get("/admin/threads").header("Authorization", "Bearer secret-token")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json");
	}

	@Test void c02_validTokenServesHeap() throws Exception {
		ca.get("/admin/heap").header("Authorization", "Bearer secret-token")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json");
	}

	@Test void c03_validTokenServesCacheFlush() throws Exception {
		ca.post("/admin/cache/flush", "").header("Authorization", "Bearer secret-token")
			.run()
			.assertStatus(200);
	}

	@Test void c04_validTokenServesHostEndpoint() throws Exception {
		ca.get("/items").header("Authorization", "Bearer secret-token")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}
}
