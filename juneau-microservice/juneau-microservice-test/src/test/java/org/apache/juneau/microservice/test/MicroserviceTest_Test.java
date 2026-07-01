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
package org.apache.juneau.microservice.test;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.test.junit.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.jetty.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end tests for {@link MicroserviceTest @MicroserviceTest} / {@link MicroserviceExtension}: boots a real
 * Jetty-backed microservice on an ephemeral port, hits it over HTTP via an injected {@link RestClient}, and
 * verifies {@link TestBean @TestBean} Mode-INJECT substitution is visible to the booted service from startup.
 */
@MicroserviceTest(configurations = MicroserviceTest_Test.AppConfig.class)
class MicroserviceTest_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Sample application under test: a @Rest resource whose response comes from an injected collaborator.
	// -----------------------------------------------------------------------------------------------------------------

	/** Collaborator the resource depends on — substituted in this test via @TestBean (Mode INJECT). */
	public interface Greeter {
		String greet();
	}

	public static class ProductionGreeter implements Greeter {
		@Override public String greet() { return "prod"; }
	}

	@Rest(paths = "/*")
	public static class GreetingResource extends RestServlet {
		private static final long serialVersionUID = 1L;

		private final Greeter greeter;

		public GreetingResource(Greeter greeter) {
			this.greeter = greeter;
		}

		@RestGet("/greeting")
		public String greeting() {
			return greeter.greet();
		}
	}

	@Configuration
	public static class AppConfig {
		@Bean public Greeter greeter() { return new ProductionGreeter(); }
		@Bean public Servlet greetingResource(Greeter greeter) { return new GreetingResource(greeter); }
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Mock-bean: a static @TestBean (CLASS scope) substitutes the Greeter BEFORE boot (Mode INJECT default).
	// -----------------------------------------------------------------------------------------------------------------

	@TestBean(scope = Scope.CLASS)
	static Greeter mockGreeter() {
		return () -> "mocked";
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A: lifecycle + parameter resolution.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_restClientInjected_hitsRealEndpoint(RestClient client) throws Exception {
		try (var resp = client.get("/greeting").run()) {
			assertEquals(200, resp.getStatusCode());
			// The @TestBean mock was injected before boot, so the resource's collaborator is the mock.
			assertEquals("mocked", resp.getBodyAsString());
		}
	}

	@Test void a02_microserviceAndBeanStoreInjected(Microservice ms, WritableBeanStore beanStore) {
		assertNotNull(ms);
		assertNotNull(beanStore);
		// The substituted collaborator is the one resolvable from the booted service's bean store.
		assertEquals("mocked", beanStore.getBean(Greeter.class).orElseThrow().greet());
	}

	@Test void a03_boundPortInjected(int port) {
		assertTrue(port > 0, "Expected an OS-assigned ephemeral port, got " + port);
	}

	@Test
	@SuppressWarnings("resource") // jsc.getServer() is the microservice's own Jetty Server; closing it from a test would be wrong.
	void a04_serverIsReallyListening(Microservice ms) {
		var jsc = ms.getBeanStore().getBean(JettyServerComponent.class).orElseThrow();
		var listening = false;
		for (var c : jsc.getServer().getConnectors())
			if (c instanceof ServerConnector sc && sc.getLocalPort() > 0)
				listening = true;
		assertTrue(listening, "Server should have a bound connector while the test class runs.");
	}
}
