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
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code ${juneau.routeindex.path:options}} SVL override on
 * {@link BasicRouteIndexResource}.
 *
 * <p>
 * Per the FINISHED-101 multi-path collapse, the historical {@code /routes} alias (formerly a
 * dual-path default on a single op) is now reached by overriding the SVL variable to
 * {@code routes}. The {@code a02} test exercises that migration scenario.
 *
 * <p>
 * Uses a fresh inner-class resource per scenario because {@link MockRestClient} caches
 * {@link RestContext} per resource class &mdash; SVL substitution is captured at
 * context-construction time.
 *
 * @since 9.5.0
 */
class BasicRouteIndexResource_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=BasicRouteIndexResource.class)
	public static class A01_OverridePath extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.routeindex.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "all-routes");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			c.get("/options").run().assertStatus(404);

			c.get("/all-routes")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json")
				.assertContent().asString().isContains("/items");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicRouteIndexResource.class)
	public static class A02_RoutesAliasMigration extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	@Test void a02_routesLegacyAliasReachableViaOverride() throws Exception {
		var key = "juneau.routeindex.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "routes");
		try {
			var c = MockRestClient.buildLax(A02_RoutesAliasMigration.class);

			c.get("/options").run().assertStatus(404);

			c.get("/routes")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("/items");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
