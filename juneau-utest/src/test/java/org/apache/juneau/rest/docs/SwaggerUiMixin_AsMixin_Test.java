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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link SwaggerUiMixin} mounted as a mixin.
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>Transitive resolution brings in {@link SwaggerMixin}: {@code /api} is mounted alongside
 * 		{@code /swagger}.
 * 	<li>{@code GET /swagger} with no {@code Accept} header renders HTML (via the {@code defaultAccept})
 * 		— this is the no-Accept-defaults-to-HTML behavior unique to UI mixins.
 * 	<li>{@code GET /swagger} with {@code Accept: application/json} returns JSON (defaultAccept doesn't
 * 		override an explicit header).
 * 	<li>The {@code /api} mount uses Juneau's standard content negotiation (no defaultAccept).
 * 	<li>Both endpoints are backed by separate sub-{@code RestContext}s per FINISHED-81.
 * </ul>
 */
class SwaggerUiMixin_AsMixin_Test extends TestBase {

	@Rest(mixins=SwaggerUiMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_swaggerUrlServesHtml_noAccept() throws Exception {
		c.get("/swagger")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("<html");
	}

	@Test void a02_swaggerUrlServesJson_jsonAccept() throws Exception {
		c.get("/swagger")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void a03_apiUrlPulledInTransitively() throws Exception {
		c.get("/api")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void a04_subContextsConstructed() throws Exception {
		MockRestClient.buildLax(A.class);
		var hostCtx = RestContext.getGlobalRegistry().get(A.class);
		var contexts = hostCtx.getMixinContexts();
		assertNotNull(contexts.get(SwaggerUiMixin.class), "SwaggerUiMixin sub-context must be present");
		assertNotNull(contexts.get(SwaggerMixin.class), "SwaggerMixin sub-context must be transitively present");
	}
}
