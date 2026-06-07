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
package org.apache.juneau.rest.server.convention;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.docs.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.swagger.*;
import org.junit.jupiter.api.*;

/**
 * Validates that all four convention-pack endpoints are excluded from the generated OpenAPI spec
 * via {@link OpSwagger#ignore() @OpSwagger(ignore=true)} on each mixin's handler.
 *
 * <p>
 * The host extends vanilla {@link RestServlet} (not {@link BasicRestServlet}) and mounts the four
 * convention mixins plus {@link OpenApiMixin} (the OpenAPI generator). The generated spec
 * must list the host's own {@code /items} endpoint but NOT the convention paths.
 *
 * @since 10.0.0
 */
class BasicConvention_OpenApiHidden_Test extends TestBase {

	@Rest(
		mixins={
			FaviconMixin.class,
			SeoMixin.class,
			VersionMixin.class,
			WellKnownMixin.class,
			OpenApiMixin.class
		},
		swaggerProvider=BasicSwaggerProvider.class
	)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/items") public String items() { return "items"; }

		@Bean public VersionMixin version() {
			return VersionMixin.create().entry("name", "convention-openapi-test").build();
		}

		@Bean public WellKnownMixin wellKnown() {
			return WellKnownMixin.create().securityTxt("Contact: x@example.com\n").build();
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_openapiSpecExcludesAllConventionPaths() throws Exception {
		var spec = c.get("/openapi.json")
			.run()
			.assertStatus(200)
			.getContent().asString();

		// Host's own endpoint must be listed.
		assertContains(spec, "/items");

		// Convention paths must NOT be listed.
		assertNotContains(spec, "/favicon.ico");
		assertNotContains(spec, "/robots.txt");
		assertNotContains(spec, "/sitemap.xml");
		assertNotContains(spec, "/version");
		assertNotContains(spec, "/info");
		assertNotContains(spec, "/about");
		assertNotContains(spec, "/.well-known/security.txt");
		assertNotContains(spec, ".well-known");
	}

	@Test void a02_conventionEndpointsStillServedDespiteHiddenFromSpec() throws Exception {
		c.get("/favicon.ico").run().assertStatus(200);
		c.get("/robots.txt").run().assertStatus(200);
		c.get("/sitemap.xml").run().assertStatus(200);
		c.get("/version").run().assertStatus(200);
		c.get("/.well-known/security.txt").run().assertStatus(200);
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
