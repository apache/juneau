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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the {@code apiFormat="both"} mode behavior:
 * {@code /api/*} keeps Swagger v2 + Swagger UI (200), AND {@code /openapi/*} is active
 * with OpenAPI 3.1 + Redoc (200).
 */
class Rest_ApiFormat_Both_Test extends TestBase {

	@Rest(apiFormat="both")
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet public String hello() { return "hello"; }
	}

	private static final MockRestClient c = MockRestClient.build(A.class);

	@Test void a01_apiServesSwagger() throws Exception {
		c.get("/api")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void a02_openapiServesOpenApi() throws Exception {
		c.get("/openapi")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\":\"3.1.0\"");
	}

	@Test void a03_apiFormatResolvesToBoth() throws Exception {
		var rc = new RestContext(new RestContext.Args(A.class, null, null, A::new, "", null));
		assertEquals("both", rc.getApiFormat());
	}
}
