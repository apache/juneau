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
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.test.junit.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end tests for {@code MockRestClient.Builder.overridingBeanStore(...)} (Phase 1 of work item 35).
 *
 * <p>
 * Builds a {@link TestBeanStore} overlay, threads it into {@link MockRestClient} via the new builder
 * method, hits an endpoint that depends on the overlaid bean, and verifies the overlay's bean is the
 * one the resource saw.
 */
// Named resource variables in tests are intentionally not closed; lifecycle is managed by test infrastructure.
@SuppressWarnings({
	"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class MockRestClient_TestOverride_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Test resource
	//-----------------------------------------------------------------------------------------------------------------

	/** Test-double interface — a stand-in for an external API the test wants to mock. */
	public interface ExternalApi {
		String describe();
	}

	@Rest(path="/api")
	public static class A_Resource {

		// The resource declares an @Bean factory for the API — production wiring.
		@Bean public ExternalApi externalApi() { return () -> "production"; }

		@RestGet(path="/who")
		public String who(ExternalApi api) {
			return api.describe();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — overlay shadows the resource's @Bean factory
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_overlay_shadows_atBeanFactory_endToEnd() throws Exception {
		var overlay = new TestBeanStore().override(ExternalApi.class, () -> "test-double");

		try (var client = MockRestClient.builder(A_Resource.class)
				.overridingBeanStore(overlay)
				.build()) {

			try (var resp = client.get("/who").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("test-double"),
					"Resource should have seen the overlay's ExternalApi instance, not production. Body: " + body);
				assertFalse(body.contains("production"),
					"Resource must not see the production @Bean ExternalApi when overlay is active. Body: " + body);
			}
		}
	}

	@Test
	void a02_overlay_namedBean_shadowsAtBeanFactory_endToEnd() throws Exception {
		var overlay = new TestBeanStore().override(ExternalApi.class, () -> "test-named", "primary");

		try (var client = MockRestClient.builder(B_NamedResource.class)
				.overridingBeanStore(overlay)
				.build()) {

			try (var resp = client.get("/named").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("test-named"),
					"Resource should have seen the overlay's named ExternalApi instance. Body: " + body);
			}
		}
	}

	@Rest(path="/api")
	public static class B_NamedResource {

		@Bean(name="primary") public ExternalApi externalApi() { return () -> "production-named"; }

		@RestGet(path="/named")
		public String named(@org.apache.juneau.commons.inject.Named("primary") ExternalApi api) {
			return api.describe();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — without the overlay, the production @Bean factory wins (regression check)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_noOverlay_productionWins() throws Exception {
		try (var client = MockRestClient.builder(A_Resource.class).build()) {
			try (var resp = client.get("/who").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("production"),
					"Without overlay, resource should see production ExternalApi. Body: " + body);
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — overlay is per-build: subsequent build with no overlay returns to production
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_overlay_isPerBuild_doesNotPollutePerClassCache() throws Exception {
		// First build with overlay — bypasses the cache entirely.
		var overlay = new TestBeanStore().override(ExternalApi.class, () -> "first-overlay");
		try (var c1 = MockRestClient.builder(C_PerBuildResource.class)
				.overridingBeanStore(overlay)
				.build()) {
			try (var resp = c1.get("/p").run()) {
				assertTrue(resp.getBodyAsString().contains("first-overlay"));
			}
		}

		// Second build with no overlay — must still see production wiring (cache or fresh, either is fine).
		try (var c2 = MockRestClient.builder(C_PerBuildResource.class).build()) {
			try (var resp = c2.get("/p").run()) {
				assertTrue(resp.getBodyAsString().contains("production-c"),
					"Cache should not have been polluted by the overlay-bearing build");
			}
		}
	}

	@Rest(path="/api")
	public static class C_PerBuildResource {

		@Bean public ExternalApi externalApi() { return () -> "production-c"; }

		@RestGet(path="/p")
		public String p(ExternalApi api) {
			return api.describe();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — classic MockRestClient also honors overridingBeanStore
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_classicMockRestClient_overridingBeanStore_endToEnd() throws Exception {
		var overlay = new TestBeanStore().override(ExternalApi.class, () -> "classic-overlay");

		var client = org.apache.juneau.rest.mock.classic.MockRestClient
			.create(D_ClassicResource.class)
			.overridingBeanStore(overlay)
			.ignoreErrors()
			.noTrace()
			.build();

		var body = client.get("/c").run().getContent().asString();
		assertTrue(body.contains("classic-overlay"),
			"Classic MockRestClient should honor overridingBeanStore. Body: " + body);
	}

	@Rest(path="/api")
	public static class D_ClassicResource {

		@Bean public ExternalApi externalApi() { return () -> "production-d"; }

		@RestGet(path="/c")
		public String c(ExternalApi api) {
			return api.describe();
		}
	}
}
