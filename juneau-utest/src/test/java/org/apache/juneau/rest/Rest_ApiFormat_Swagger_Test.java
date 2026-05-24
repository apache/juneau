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
 * Verifies the default {@code apiFormat="swagger"} mode behavior:
 * {@code /api/*} serves Swagger v2 (200) and {@code /openapi/*} returns 404.
 */
class Rest_ApiFormat_Swagger_Test extends TestBase {

	@Rest
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet public String hello() { return "hello"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_apiServesSwagger() throws Exception {
		c.get("/api")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void a02_openapiReturns404() throws Exception {
		c.get("/openapi?noTrace=true")
			.accept("application/json")
			.run()
			.assertStatus(404);
	}

	@Test void a03_apiFormatResolvesToSwagger() throws Exception {
		var rc = new RestContext(new RestContext.Args(A.class, null, null, A::new, "", null, null, null));
		assertEquals("swagger", rc.getApiFormat());
	}
}
