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
package org.apache.juneau.rest.server.staticfile;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.docs.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.swagger.*;
import org.junit.jupiter.api.*;

/**
 * Validates that the static-files mixin's greedy {@code /static/*} handler is excluded from the
 * generated Swagger/OpenAPI spec, per {@link OpSwagger#ignore() @OpSwagger(ignore=true)} on
 * {@link StaticFilesMixin#getStaticFile} and the matching HEAD handler.
 *
 * <p>
 * The host below mounts both {@link StaticFilesMixin} (static-file serving) and
 * {@link OpenApiMixin} (OpenAPI generator) as mixins on a vanilla {@link RestServlet}
 * (NOT {@link BasicRestServlet}) so the legacy
 * {@code BasicRestOperations.getHtdoc(...)} method does not pollute the spec independently
 * of what the mixin emits. The generated spec must list the host's own {@code /items} endpoint
 * but NOT the static-file mounts contributed by the mixin.
 *
 * @since 10.0.0
 */
class StaticFilesMixin_OpenApiHidden_Test extends TestBase {

	/**
	 * Host extends vanilla {@link RestServlet} (NOT {@link BasicRestServlet}) so the legacy
	 * {@code getHtdoc(...)} from {@code BasicRestOperations} does not pollute the spec
	 * independently of the mixin. The spec generator needs a {@code SwaggerProvider} bean to
	 * generate; we wire it explicitly via {@link Rest#swaggerProvider}.
	 */
	@Rest(
		mixins={StaticFilesMixin.class, OpenApiMixin.class},
		swaggerProvider=BasicSwaggerProvider.class
	)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_openapiSpecExcludesStaticPaths() throws Exception {
		// /openapi.json is format-pinned to JSON regardless of Accept (per OpenApiMixin); // NOSONAR
		// avoids the need to register JSON serializers on the vanilla RestServlet host.
		var spec = c.get("/openapi.json")
			.run()
			.assertStatus(200)
			.getContent().asString();

		// Sanity: host's own endpoint must be present.
		assertContains(spec, "/items");
		// Static-file mounts must NOT be present.
		assertNotContains(spec, "/static");
		assertNotContains(spec, "/htdocs");
	}

	@Test void a02_staticFilesStillServedDespiteHiddenFromSpec() throws Exception {
		c.get("/static/javadoc.css")
			.run()
			.assertStatus(200);
		// FINISHED-101: /htdocs/* is no longer a multi-path default; migration covered by
		// StaticFilesMixin_SvlPathOverride_Test#a02.
		c.get("/htdocs/javadoc.css")
			.run()
			.assertStatus(404);
	}

	/**
	 * Host that exercises the LEGACY static-file path: a {@link BasicRestServlet} subclass with
	 * the api-docs mixin pack inherited and no explicit {@link StaticFilesMixin}. The
	 * legacy {@code getHtdoc(...)} method on {@code BasicRestOperations} now carries
	 * {@code @OpSwagger(ignore=true)} so {@code /htdocs/*} must NOT appear in the generated spec
	 * even when the new mixin is not mounted.
	 */
	@Rest
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_legacyGetHtdocAlsoHiddenFromSpec() throws Exception {
		var spec = cb.get("/openapi.json")
			.run()
			.assertStatus(200)
			.getContent().asString();

		// Sanity: host's own endpoint must be present.
		assertContains(spec, "/items");
		// Legacy /htdocs/* must NOT be present after the @OpSwagger(ignore=true) cleanup.
		assertNotContains(spec, "/htdocs");
	}

	@Test void b02_legacyHtdocStillServedDespiteHiddenFromSpec() throws Exception {
		// Legacy getHtdoc(...) handler still serves the file even though it's hidden from spec.
		cb.get("/htdocs/javadoc.css")
			.run()
			.assertStatus(200);
	}

	private static void assertContains(String s, String needle) {
		if (!s.contains(needle))
			throw new AssertionError("Expected to contain '" + needle + "' but did not. Body: " + s);
	}

	private static void assertNotContains(String s, String needle) {
		if (s.contains(needle))
			throw new AssertionError("Expected NOT to contain '" + needle + "' but did. Body excerpt: "
				+ s.substring(Math.max(0, s.indexOf(needle) - 50), Math.min(s.length(), s.indexOf(needle) + 100)));
	}
}
