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
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@code @Rest(mixins=...)} aggregates across the parent-class chain &mdash; a parent's
 * mixin contributions and a child's mixin contributions stack rather than override.
 *
 * <p>
 * Setup:
 * <ul>
 * 	<li>{@code Parent} declares {@code @Rest(mixins=BasicSwaggerUiResource.class)}.
 * 	<li>{@code Child} extends {@code Parent} and declares {@code @Rest(mixins=BasicRedocResource.class)}.
 * </ul>
 *
 * <p>
 * Acceptance:
 * <ul>
 * 	<li>{@code Child}'s {@code mixinContexts} map contains all four mixin classes
 * 		({@code BasicSwaggerUiResource}, {@code BasicSwaggerResource} via transitive,
 * 		{@code BasicRedocResource}, {@code BasicOpenApiResource} via transitive).
 * 	<li>All four URL paths resolve correctly.
 * </ul>
 */
class BasicApiDocs_ParentChain_Test extends TestBase {

	@Rest(mixins=BasicSwaggerUiResource.class)
	public static class Parent extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Rest(mixins=BasicRedocResource.class)
	public static class Child extends Parent {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(Child.class);

	@Test void a01_parentChainAggregatesAllMixins() throws Exception {
		MockRestClient.buildLax(Child.class);
		var hostCtx = RestContext.getGlobalRegistry().get(Child.class);
		var contexts = hostCtx.getMixinContexts();

		assertNotNull(contexts.get(BasicSwaggerUiResource.class), "Parent's mixin must propagate to child");
		assertNotNull(contexts.get(BasicSwaggerResource.class), "Parent's transitive mixin must propagate to child");
		assertNotNull(contexts.get(BasicRedocResource.class), "Child's mixin must be present");
		assertNotNull(contexts.get(BasicOpenApiResource.class), "Child's transitive mixin must be present");

		assertEquals(4, contexts.size(),
			"Expected exactly four mixin contexts; got: " + contexts.keySet());
	}

	@Test void a02_apiFromParentChain() throws Exception {
		c.get("/api").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void a03_swaggerFromParentChain() throws Exception {
		c.get("/swagger").run().assertStatus(200).assertContent().asString().isContains("<html");
	}

	@Test void a04_openapiFromChildChain() throws Exception {
		c.get("/openapi").accept("application/json").run().assertStatus(200)
			.assertContent().asString().isContains("\"openapi\"");
	}

	@Test void a05_redocFromChildChain() throws Exception {
		c.get("/redoc").run().assertStatus(200).assertContent().asString().isContains("<html");
	}
}
