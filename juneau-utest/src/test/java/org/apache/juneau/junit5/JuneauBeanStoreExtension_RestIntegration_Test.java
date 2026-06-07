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
package org.apache.juneau.junit5;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

/**
 * End-to-end test of {@link JuneauBeanStoreExtension} + {@code @TestBean} flowing through a real
 * {@code MockRestClient} pipeline.
 *
 * <p>
 * Validates the canonical "Mode INJECT" wiring path:
 * <ol>
 * 	<li>Test class is annotated with {@code @ExtendWith(JuneauBeanStoreExtension.class)} (or registers it
 * 		programmatically).
 * 	<li>{@code @TestBean} fields/methods declare per-method or per-class overrides.
 * 	<li>The test grabs the overlay via {@code TestBeanStore} parameter resolution or via the extension's
 * 		{@code getStore()} accessor.
 * 	<li>The overlay is threaded into {@code MockRestClient.builder(...).overridingBeanStore(overlay)}.
 * 	<li>The resource sees the overlaid bean, not its own {@code @Bean} factory binding.
 * </ol>
 */
@ExtendWith(JuneauBeanStoreExtension.class)
class JuneauBeanStoreExtension_RestIntegration_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test resource — declares a production @Bean factory; the test will overlay it.
	//-----------------------------------------------------------------------------------------------------------------

	public interface ExternalApi {
		String describe();
	}

	@Rest(path = "/api")
	public static class MyResource {

		@Bean
		public ExternalApi externalApi() { return () -> "production"; }

		@RestGet(path = "/who")
		public String who(ExternalApi api) {
			return api.describe();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — @TestBean field + parameter-resolved store + MockRestClient.overridingBeanStore
	//-----------------------------------------------------------------------------------------------------------------

	@TestBean
	ExternalApi mockApi = () -> "test-double";

	@Test
	void a01_testBeanField_flowsToResource(TestBeanStore store) throws Exception {
		try (var client = MockRestClient.builder(MyResource.class)
				.overridingBeanStore(store)
				.build()) {
			try (var resp = client.get("/who").run()) {
				assertEquals(200, resp.getStatusCode());
				var body = resp.getBodyAsString();
				assertTrue(body.contains("test-double"),
					"Resource should have seen the @TestBean field, not production. Body: " + body);
				assertFalse(body.contains("production"),
					"Resource must not see the production @Bean ExternalApi when overlay is active. Body: " + body);
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — programmatic registration via @RegisterExtension exposes getStore() to @BeforeEach bodies
	//-----------------------------------------------------------------------------------------------------------------

	@Nested
	class B_Programmatic {

		@RegisterExtension
		final JuneauBeanStoreExtension ext = JuneauBeanStoreExtension.create();

		@TestBean
		ExternalApi anotherMock = () -> "programmatic-double";

		@Test
		void b01_programmaticRegistration_getStore_flowsToResource() throws Exception {
			try (var client = MockRestClient.builder(MyResource.class)
					.overridingBeanStore(ext.getStore())
					.build()) {
				try (var resp = client.get("/who").run()) {
					assertEquals(200, resp.getStatusCode());
					assertTrue(resp.getBodyAsString().contains("programmatic-double"));
				}
			}
		}
	}
}
