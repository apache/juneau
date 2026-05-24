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
package org.apache.juneau.rest.docs;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicOpenApiResource} mounted as a mixin via {@code @Rest(mixins=...)} on a
 * vanilla {@link RestServlet}.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>{@code GET /openapi} (Accept JSON) returns the OpenAPI 3.1 spec.
 * 	<li>{@code GET /openapi.json} returns JSON regardless of {@code Accept} (format-pinned).
 * 	<li>{@code GET /openapi.yaml} returns YAML regardless of {@code Accept} (format-pinned).
 * 	<li>The host's own endpoints are unaffected.
 * </ul>
 */
class BasicOpenApiResource_AsMixin_Test extends TestBase {

	@Rest(mixins=BasicOpenApiResource.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_openapiServesJsonAccept() throws Exception {
		c.get("/openapi")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a02_openapiJsonIsPinnedToJson_evenWithHtmlAccept() throws Exception {
		c.get("/openapi.json")
			.accept("text/html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a03_openapiJsonIsPinnedToJson_noAccept() throws Exception {
		c.get("/openapi.json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a04_openapiYamlIsPinnedToYaml_evenWithHtmlAccept() throws Exception {
		c.get("/openapi.yaml")
			.accept("text/html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("openapi:");
	}

	@Test void a05_openapiYamlIsPinnedToYaml_noAccept() throws Exception {
		c.get("/openapi.yaml")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("openapi:");
	}

	@Test void a06_hostEndpointStillReachable() throws Exception {
		c.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}
}
