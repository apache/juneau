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

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the OQ8 query-param mirrors on group resources: <c>GET /?Swagger</c> returns the
 * Swagger v2 document and <c>GET /?OpenApi</c> returns the OpenAPI 3.1 document, alongside the
 * standard navigation page rendered by <c>GET /</c> without those query parameters. Each handler
 * honors {@link Rest#apiFormat()} so the API-format precedence rules also apply to
 * the group resource inline mirrors.
 */
class Rest_GroupQueryMirrors_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Default (apiFormat="swagger"): ?Swagger serves Swagger v2; ?OpenApi returns 404.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(children={A_Child.class})
	public static class A_Default extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(path="/child")
	public static class A_Child extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet public String hello() { return "hello"; }
	}

	private static final MockRestClient cDefault = MockRestClient.buildLax(A_Default.class);

	@Test void a01_root_returnsChildResourceDescriptions() throws Exception {
		cDefault.get("/")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"name\":\"child\"");
	}

	@Test void a02_rootSwaggerQuery_returnsSwagger() throws Exception {
		cDefault.get("/?Swagger=true")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void a03_rootOpenApiQuery_returns404() throws Exception {
		cDefault.get("/?OpenApi=true&noTrace=true")
			.accept("application/json")
			.run()
			.assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// apiFormat="openapi": ?OpenApi serves OpenAPI 3.1; ?Swagger returns 404.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(apiFormat="openapi", children={B_Child.class})
	public static class B_OpenApi extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(path="/child")
	public static class B_Child extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet public String hello() { return "hello"; }
	}

	private static final MockRestClient cOpenApi = MockRestClient.buildLax(B_OpenApi.class);

	@Test void b01_rootOpenApiQuery_returnsOpenApi() throws Exception {
		cOpenApi.get("/?OpenApi=true")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\":\"3.1.0\"");
	}

	@Test void b02_rootSwaggerQuery_returns404() throws Exception {
		cOpenApi.get("/?Swagger=true&noTrace=true")
			.accept("application/json")
			.run()
			.assertStatus(404);
	}

	//------------------------------------------------------------------------------------------------------------------
	// apiFormat="both": both ?Swagger and ?OpenApi serve their respective documents.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(apiFormat="both", children={C_Child.class})
	public static class C_Both extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;
	}

	@Rest(path="/child")
	public static class C_Child extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet public String hello() { return "hello"; }
	}

	private static final MockRestClient cBoth = MockRestClient.build(C_Both.class);

	@Test void c01_rootSwaggerQuery_returnsSwagger() throws Exception {
		cBoth.get("/?Swagger=true")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void c02_rootOpenApiQuery_returnsOpenApi() throws Exception {
		cBoth.get("/?OpenApi=true")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\":\"3.1.0\"");
	}

	@Test void c03_root_returnsChildResourceDescriptions() throws Exception {
		cBoth.get("/")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"name\":\"child\"");
	}

	//------------------------------------------------------------------------------------------------------------------
	// BasicRestObjectGroup (POJO sibling): same query-mirror behavior.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(apiFormat="both", children={D_Child.class})
	public static class D_PojoGroup extends BasicRestObjectGroup {
		// No serialVersionUID — BasicRestObjectGroup is not a Servlet.
	}

	@Rest(path="/child")
	public static class D_Child extends BasicRestObject {
		@RestGet public String hello() { return "hello"; }
	}

	private static final MockRestClient cPojo = MockRestClient.build(D_PojoGroup.class);

	@Test void d01_rootSwaggerQuery_returnsSwagger() throws Exception {
		cPojo.get("/?Swagger=true")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"swagger\":\"2.0\"");
	}

	@Test void d02_rootOpenApiQuery_returnsOpenApi() throws Exception {
		cPojo.get("/?OpenApi=true")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"openapi\":\"3.1.0\"");
	}
}
