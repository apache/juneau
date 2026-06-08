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
import org.apache.juneau.rest.server.convention.*;
import org.apache.juneau.rest.server.ops.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies transitive mixin resolution dedupes correctly when a host composes the two UI mixins
 * together.
 *
 * <p>
 * {@code @Rest(mixins={SwaggerUiMixin.class, RedocMixin.class})} should pull in
 * {@code SwaggerMixin} (via {@code SwaggerUiMixin}) and
 * {@code OpenApiMixin} (via {@code RedocMixin}) exactly once each &mdash; not twice,
 * even though the user could equivalently have listed all four mixins explicitly.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>The four api-docs mixin classes are present in the host's {@code mixinContexts} map, alongside the
 * 		four residual op-mixins ({@code ErrorMixin}, {@code HtdocMixin}, {@code StatsMixin},
 * 		{@code FaviconMixin}) contributed by {@code BasicRestServlet}.
 * 	<li>Each mixin appears exactly once (no duplicates) &mdash; eight in total.
 * 	<li>All six api-docs URL paths resolve correctly.
 * </ul>
 */
@SuppressWarnings({
	"resource" // Static MockRestClient fields are a common test pattern; resources are managed by the mock framework.
})
class BasicApiDocs_TransitiveDedupe_Test extends TestBase {

	@Rest(mixins={SwaggerUiMixin.class, RedocMixin.class})
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_allMixinContextsPresent() throws Exception {
		MockRestClient.buildLax(A.class);
		var hostCtx = RestContext.getGlobalRegistry().get(A.class);
		var contexts = hostCtx.getMixinContexts();

		// api-docs pack — deduped from A's explicit SwaggerUiMixin/RedocMixin + BasicRestServlet's own list
		// plus the transitive SwaggerMixin/OpenApiMixin pulls (each present exactly once).
		assertNotNull(contexts.get(SwaggerUiMixin.class));
		assertNotNull(contexts.get(SwaggerMixin.class));
		assertNotNull(contexts.get(RedocMixin.class));
		assertNotNull(contexts.get(OpenApiMixin.class));
		// Residual op-mixins contributed by BasicRestServlet.
		assertNotNull(contexts.get(ErrorMixin.class));
		assertNotNull(contexts.get(HtdocMixin.class));
		assertNotNull(contexts.get(StatsMixin.class));
		assertNotNull(contexts.get(FaviconMixin.class));

		assertEquals(8, contexts.size(),
			"Expected exactly eight mixin contexts (no duplicates); got: " + contexts.keySet());
	}

	@Test void a02_apiPathReachable() throws Exception {
		c.get("/api").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void a03_swaggerPathReachable() throws Exception {
		c.get("/swagger").run().assertStatus(200).assertContent().asString().isContains("<html");
	}

	@Test void a04_openapiPathReachable() throws Exception {
		c.get("/openapi").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a05_openapiJsonPathReachable() throws Exception {
		c.get("/openapi.json").run().assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a06_openapiYamlPathReachable() throws Exception {
		c.get("/openapi.yaml").run().assertStatus(200)
			.assertContent().asString().isContains("openapi:");
	}

	@Test void a07_redocPathReachable() throws Exception {
		c.get("/redoc").run().assertStatus(200).assertContent().asString().isContains("<html");
	}
}
