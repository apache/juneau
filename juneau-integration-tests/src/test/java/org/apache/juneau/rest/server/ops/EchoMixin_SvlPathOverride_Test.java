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
package org.apache.juneau.rest.server.ops;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Validates the {@code ${juneau.echo.path:echo}} SVL override on {@link EchoMixin}.
 *
 * <p>
 * Per the multi-path collapse, the historical {@code /debug/echo/*} alias (formerly
 * a dual-path default on a single op) is now reached by overriding the SVL variable to
 * {@code debug/echo}. The {@code a02} test exercises that migration scenario.
 *
 * <p>
 * Each host explicitly enables the endpoint via a {@code @Bean EchoMixin.create().enabled()} factory so the
 * (default-OFF) endpoint serves &mdash; reachability is decoupled from the JUL logger level (see
 * {@link EchoMixin_Enablement_Test}). The default-deny behavior is exercised separately in the AsMixin suite.
 *
 * <p>
 * Uses a fresh inner-class resource per scenario because {@link MockRestClient} caches
 * {@link RestContext} per resource class &mdash; SVL substitution is captured at
 * context-construction time.
 *
 * @since 10.0.0
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class EchoMixin_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=EchoMixin.class)
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() { return EchoMixin.create().enabled().build(); }
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "introspect");
		try {
			var c = MockRestClient.createLax(A01_OverridePath.class).build();

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

	@Rest(mixins=EchoMixin.class)
	public static class A02_DebugEchoAliasMigration extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() { return EchoMixin.create().enabled().build(); }
	}

	@Test void a02_debugEchoLegacyAliasReachableViaOverride() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "debug/echo");
		try {
			var c = MockRestClient.createLax(A02_DebugEchoAliasMigration.class).build();

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

	@Rest(mixins=EchoMixin.class)
	public static class A03_BareToken extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() { return EchoMixin.create().enabled().build(); }
	}

	@Test void a03_overrideBareToken() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx");
		try {
			var c = MockRestClient.createLax(A03_BareToken.class).build();
			c.get("/xxx/ping").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=EchoMixin.class)
	public static class A04_LeadingSlash extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() { return EchoMixin.create().enabled().build(); }
	}

	@Test void a04_overrideLeadingSlash() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx");
		try {
			var c = MockRestClient.createLax(A04_LeadingSlash.class).build();
			c.get("/xxx/ping").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=EchoMixin.class)
	public static class A05_TrailingSlash extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() { return EchoMixin.create().enabled().build(); }
	}

	@Test void a05_overrideTrailingSlash() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx/");
		try {
			var c = MockRestClient.createLax(A05_TrailingSlash.class).build();
			c.get("/xxx/ping").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=EchoMixin.class)
	public static class A06_BothSlashes extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() { return EchoMixin.create().enabled().build(); }
	}

	@Test void a06_overrideBothSlashes() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/");
		try {
			var c = MockRestClient.createLax(A06_BothSlashes.class).build();
			c.get("/xxx/ping").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=EchoMixin.class)
	public static class A07_WildcardSuffix extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() { return EchoMixin.create().enabled().build(); }
	}

	@Test void a07_overrideWildcardSuffix() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/*");
		try {
			var c = MockRestClient.createLax(A07_WildcardSuffix.class).build();
			c.get("/xxx/ping").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=EchoMixin.class)
	public static class A08_MultiSegment extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() { return EchoMixin.create().enabled().build(); }
	}

	@Test void a08_overrideMultiSegment() throws Exception {
		var key = "juneau.echo.path";
		var prev = System.getProperty(key);
		// Use debug/echo2 (2-segment) to test multi-segment path normalization while staying
		// under the 2-segment SVL path depth that Juneau's UrlPathMatcher reliably supports
		// for mixin op-paths. Paths deeper than 2 segments are not tested here to avoid
		// collision with SwaggerMixin's /api/* default and potential routing edge cases
		// with 3+ segment base paths in SVL-resolved @RestOp patterns.
		System.setProperty(key, "/debug/echo2/*");
		try {
			var c = MockRestClient.createLax(A08_MultiSegment.class).build();
			c.get("/debug/echo2/ping").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
