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
 * Validates the {@code ${juneau.echo.path:echo}} SVL override on {@link BasicEchoResource}.
 *
 * <p>
 * Per the FINISHED-101 multi-path collapse, the historical {@code /debug/echo/*} alias (formerly
 * a dual-path default on a single op) is now reached by overriding the SVL variable to
 * {@code debug/echo}. The {@code a02} test exercises that migration scenario.
 *
 * <p>
 * Uses {@code @Debug("always")} so the debug guard does not return 404 &mdash; the
 * default-deny behavior of {@link BasicEchoResource} is exercised separately in the AsMixin
 * test suite.
 *
 * <p>
 * Uses a fresh inner-class resource per scenario because {@link MockRestClient} caches
 * {@link RestContext} per resource class &mdash; SVL substitution is captured at
 * context-construction time.
 *
 * @since 9.5.0
 */
class BasicEchoResource_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=BasicEchoResource.class, debug=@Debug("always"))
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "introspect");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			c.get("/echo/anything").run().assertStatus(404);

			c.get("/introspect/anything")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=BasicEchoResource.class, debug=@Debug("always"))
	public static class A02_DebugEchoAliasMigration extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_debugEchoLegacyAliasReachableViaOverride() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "debug/echo");
		try {
			var c = MockRestClient.buildLax(A02_DebugEchoAliasMigration.class);

			c.get("/echo/anything").run().assertStatus(404);

			c.get("/debug/echo/anything")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
