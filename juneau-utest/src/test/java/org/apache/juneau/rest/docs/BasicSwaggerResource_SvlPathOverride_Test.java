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
 * Validates the {@code ${juneau.swagger.path:api}} SVL override on {@link BasicSwaggerResource}.
 *
 * <p>
 * Uses a fresh inner-class resource because {@link MockRestClient} caches {@link RestContext} per
 * resource class &mdash; SVL substitution is captured at context-construction time, so the same
 * resource class with a different system property would otherwise hit the cached resolution and
 * silently mask the override.
 *
 * @since 9.5.0
 */
class BasicSwaggerResource_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=BasicSwaggerResource.class)
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.swagger.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "custom-api");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			// Default /api/* should now 404.
			c.get("/api")
				.accept("application/json")
				.run()
				.assertStatus(404);

			// Overridden /custom-api/* should serve the Swagger v2 spec.
			c.get("/custom-api")
				.accept("application/json")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("\"swagger\":\"2.0\"");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
