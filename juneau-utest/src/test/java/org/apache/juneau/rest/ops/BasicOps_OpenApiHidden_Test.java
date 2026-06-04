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
package org.apache.juneau.rest.ops;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.docs.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.swagger.*;
import org.junit.jupiter.api.*;

/**
 * Validates that all ops-pack endpoints are excluded from the generated OpenAPI spec via
 * {@link OpSwagger#ignore() @OpSwagger(ignore=true)} on each mixin's handler.
 *
 * <p>
 * The host extends vanilla {@link RestServlet} and mounts the three ops mixins plus
 * {@link OpenApiMixin} (the OpenAPI generator). The generated spec must list the host's
 * own {@code /items} endpoint but NOT the ops-pack paths.
 *
 * @since 10.0.0
 */
class BasicOps_OpenApiHidden_Test extends TestBase {

	@Rest(
		mixins={
			EchoMixin.class,
			AdminMixin.class,
			RouteIndexMixin.class,
			OpenApiMixin.class
		},
		debug=@Debug("always"),
		swaggerProvider=BasicSwaggerProvider.class
	)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/items") public String items() { return "items"; }

		// Allow-all guards so the admin paths can serve at all (independent of OpenAPI hide).
		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	@Test void a01_openapiSpecExcludesAllOpsPaths() throws Exception {
		var spec = c.get("/openapi.json")
			.run()
			.assertStatus(200)
			.getContent().asString();

		// Host's own endpoint must be listed.
		assertContains(spec, "/items");

		// Ops paths must NOT be listed.
		assertNotContains(spec, "/echo/");
		assertNotContains(spec, "/debug/echo/");
		assertNotContains(spec, "/admin/threads");
		assertNotContains(spec, "/admin/heap");
		assertNotContains(spec, "/admin/cache/flush");
		assertNotContains(spec, "/admin/ratelimit");
		assertNotContains(spec, "\"/options\"");
		assertNotContains(spec, "\"/routes\"");
	}

	@Test void a02_opsEndpointsStillServedDespiteHiddenFromSpec() throws Exception {
		c.get("/echo/x").run().assertStatus(200);
		c.get("/options").run().assertStatus(200);
		c.get("/admin/threads").run().assertStatus(200);
		// FINISHED-101: /routes is no longer a multi-path default; migration covered by the
		// per-mixin *_SvlPathOverride_Test classes.
		c.get("/routes").run().assertStatus(404);
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
