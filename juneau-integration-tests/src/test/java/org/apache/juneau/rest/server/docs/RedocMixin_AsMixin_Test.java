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
package org.apache.juneau.rest.server.docs;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link RedocMixin} mounted as a mixin.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>Transitive resolution brings in {@link OpenApiMixin}: {@code /openapi}, {@code /openapi.json},
 * 		and {@code /openapi.yaml} are all mounted alongside {@code /redoc}.
 * 	<li>{@code GET /redoc} with no {@code Accept} header renders HTML (via {@code defaultAccept}).
 * 	<li>{@code GET /redoc} with {@code Accept: application/json} returns the OpenAPI spec as JSON
 * 		({@code defaultAccept} doesn't override an explicit header).
 * 	<li>Both endpoints are backed by separate sub-{@code RestContext}s.
 * </ul>
 */
class RedocMixin_AsMixin_Test extends TestBase {

	@Rest(mixins=RedocMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_redocServesHtml_noAccept() throws Exception {
		c.get("/redoc")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("<html");
	}

	@Test void a02_redocServesJson_jsonAccept() throws Exception {
		c.get("/redoc")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a03_openapiPulledInTransitively() throws Exception {
		c.get("/openapi")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a04_openapiJsonPulledInTransitively() throws Exception {
		c.get("/openapi.json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a05_openapiYamlPulledInTransitively() throws Exception {
		c.get("/openapi.yaml")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("openapi:");
	}

	@Test void a06_subContextsConstructed() {
		MockRestClient.buildLax(A.class);
		var hostCtx = RestContext.getGlobalRegistry().get(A.class);
		var contexts = hostCtx.getMixinContexts();
		assertNotNull(contexts.get(RedocMixin.class), "RedocMixin sub-context must be present");
		assertNotNull(contexts.get(OpenApiMixin.class), "OpenApiMixin sub-context must be transitively present");
	}
}
