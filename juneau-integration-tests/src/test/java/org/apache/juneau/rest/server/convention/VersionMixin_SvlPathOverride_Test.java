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
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code ${juneau.version.path:version}} SVL override on
 * {@link VersionMixin}.
 *
 * <p>
 * Per the FINISHED-101 multi-path collapse, the historical {@code /info} and {@code /about}
 * aliases (formerly a triple-path default on a single op) are now reached by overriding the SVL
 * variable to {@code info} or {@code about}. The {@code a02} test exercises the {@code /info}
 * alias migration scenario.
 *
 * <p>
 * Uses a fresh inner-class resource per scenario because {@link MockRestClient} caches
 * {@link RestContext} per resource class &mdash; SVL substitution is captured at
 * context-construction time.
 *
 * @since 10.0.0
 */
class VersionMixin_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=VersionMixin.class)
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.version.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "build-info");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			c.get("/version").accept("application/json").run().assertStatus(404);

			c.get("/build-info")
				.accept("application/json")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json")
				.assertContent().asString().isContains("javaVersion");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=VersionMixin.class)
	public static class A02_InfoAliasMigration extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_infoLegacyAliasReachableViaOverride() throws Exception {
		var key = "juneau.version.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "info");
		try {
			var c = MockRestClient.buildLax(A02_InfoAliasMigration.class);

			c.get("/version").accept("application/json").run().assertStatus(404);

			c.get("/info")
				.accept("application/json")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json")
				.assertContent().asString().isContains("javaVersion");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=VersionMixin.class)
	public static class A03_BareToken extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03_overrideBareToken() throws Exception {
		var key = "juneau.version.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx");
		try {
			var c = MockRestClient.buildLax(A03_BareToken.class);
			c.get("/xxx").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("javaVersion");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=VersionMixin.class)
	public static class A04_LeadingSlash extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a04_overrideLeadingSlash() throws Exception {
		var key = "juneau.version.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx");
		try {
			var c = MockRestClient.buildLax(A04_LeadingSlash.class);
			c.get("/xxx").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("javaVersion");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=VersionMixin.class)
	public static class A05_TrailingSlash extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a05_overrideTrailingSlash() throws Exception {
		var key = "juneau.version.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx/");
		try {
			var c = MockRestClient.buildLax(A05_TrailingSlash.class);
			c.get("/xxx").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("javaVersion");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=VersionMixin.class)
	public static class A06_BothSlashes extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a06_overrideBothSlashes() throws Exception {
		var key = "juneau.version.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/");
		try {
			var c = MockRestClient.buildLax(A06_BothSlashes.class);
			c.get("/xxx").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("javaVersion");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=VersionMixin.class)
	public static class A07_MultiSegment extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a07_overrideMultiSegment() throws Exception {
		var key = "juneau.version.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/api/v1/xxx");
		try {
			var c = MockRestClient.buildLax(A07_MultiSegment.class);
			c.get("/api/v1/xxx").accept("application/json").run().assertStatus(200).assertContent().asString().isContains("javaVersion");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
