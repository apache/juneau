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

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.*;

/**
 * Verifies the {@code static Microservice.Builder} supplier path of {@link MicroserviceTest @MicroserviceTest}
 * (SUT specified via a builder method rather than {@code configurations=}), and that a second
 * {@code @MicroserviceTest} class boots + tears down cleanly in the same JVM (isolation from
 * {@link MicroserviceTest_Test}).
 */
@MicroserviceTest
class MicroserviceTestBuilderSupplier_Test extends TestBase {

	@Rest(paths = "/*")
	public static class PingResource extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/ping")
		public String ping() {
			return "pong";
		}
	}

	@Configuration
	public static class PingConfig {
		@Bean public Servlet pingResource() { return new PingResource(); }
	}

	/** Builder-supplier discovered by the default name {@code microserviceBuilder} (Q1 SUT-spec option b). */
	static Microservice.Builder microserviceBuilder() {
		return Microservice.create().configurations(PingConfig.class);
	}

	@Test void a01_builderSupplierBootsAndServes(RestClient client) throws Exception {
		try (var resp = client.get("/ping").run()) {
			assertEquals(200, resp.getStatusCode());
			assertEquals("pong", resp.getBodyAsString());
		}
	}

	@Test void a02_freshInstancePerClass(Microservice ms) {
		assertNotNull(ms);
		assertNotNull(ms.getBeanStore());
	}
}
