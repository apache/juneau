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
 * Validates the {@code ${juneau.openapi.path:openapi}} SVL override on
 * {@link BasicOpenApiResource}.
 *
 * <p>
 * The same variable is reused across all three op-paths on this mixin
 * ({@code /openapi/*}, {@code /openapi.json}, {@code /openapi.yaml}) so a single override
 * relocates the whole surface. The test exercises one of those (the format-pinned JSON mount)
 * as the canonical end-to-end check; the other two paths share the same SVL substitution code
 * path.
 *
 * <p>
 * Uses a fresh inner-class resource because {@link MockRestClient} caches {@link RestContext} per
 * resource class &mdash; SVL substitution is captured at context-construction time.
 *
 * @since 9.5.0
 */
class BasicOpenApiResource_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=BasicOpenApiResource.class)
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.openapi.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "custom-openapi");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			// All three default paths now 404.
			c.get("/openapi.json").run().assertStatus(404);
			c.get("/openapi.yaml").run().assertStatus(404);
			c.get("/openapi").accept("application/json").run().assertStatus(404);

			// All three overridden paths serve.
			c.get("/custom-openapi.json")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("\"openapi\"");
			c.get("/custom-openapi.yaml")
				.run()
				.assertStatus(200);
			c.get("/custom-openapi")
				.accept("application/json")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("\"openapi\"");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
