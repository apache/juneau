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
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Validates the {@code ${juneau.admin.path:admin}} SVL prefix override on
 * {@link AdminMixin}.
 *
 * <p>
 * Unlike the single-op mixins, {@link AdminMixin} declares <i>four</i> distinct ops
 * (threads / heap / cache/flush / ratelimit) sharing a common prefix variable. A single
 * override relocates the whole admin surface; the test exercises {@code GET .../threads} as
 * the canonical end-to-end check since the other three ops share the same SVL substitution
 * code path.
 *
 * <p>
 * Uses an allow-all {@link RestGuardList} so the {@link DenyAllGuard} default does not return
 * 403 ahead of the path match. Uses a fresh inner-class resource because {@link MockRestClient}
 * caches {@link RestContext} per resource class &mdash; SVL substitution is captured at
 * context-construction time.
 *
 * @since 10.0.0
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class AdminMixin_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=AdminMixin.class)
	public static class A01_OverridePath extends RestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public RestGuardList guards(BeanStore bs) {
			return RestGuardList.create(bs).build();
		}
	}

	@Test void a01_svlOverrideChangesPathPrefix() throws Exception {
		var key = "juneau.admin.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "ops");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			c.get("/admin/threads").run().assertStatus(404);
			c.get("/admin/heap").run().assertStatus(404);

			c.get("/ops/threads")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json");
			c.get("/ops/heap")
				.run()
				.assertStatus(200)
				.assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=AdminMixin.class)
	public static class A02_BareToken extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }
	}

	@Test void a02_overrideBareToken() throws Exception {
		var key = "juneau.admin.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx");
		try {
			var c = MockRestClient.buildLax(A02_BareToken.class);
			c.get("/xxx/threads").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=AdminMixin.class)
	public static class A03_LeadingSlash extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }
	}

	@Test void a03_overrideLeadingSlash() throws Exception {
		var key = "juneau.admin.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx");
		try {
			var c = MockRestClient.buildLax(A03_LeadingSlash.class);
			c.get("/xxx/threads").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=AdminMixin.class)
	public static class A04_TrailingSlash extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }
	}

	@Test void a04_overrideTrailingSlash() throws Exception {
		var key = "juneau.admin.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx/");
		try {
			var c = MockRestClient.buildLax(A04_TrailingSlash.class);
			c.get("/xxx/threads").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=AdminMixin.class)
	public static class A05_BothSlashes extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }
	}

	@Test void a05_overrideBothSlashes() throws Exception {
		var key = "juneau.admin.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/");
		try {
			var c = MockRestClient.buildLax(A05_BothSlashes.class);
			c.get("/xxx/threads").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=AdminMixin.class)
	public static class A06_WildcardSuffix extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }
	}

	@Test void a06_overrideWildcardSuffix() throws Exception {
		var key = "juneau.admin.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/*");
		try {
			var c = MockRestClient.buildLax(A06_WildcardSuffix.class);
			c.get("/xxx/threads").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=AdminMixin.class)
	public static class A07_MultiSegment extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public RestGuardList guards(BeanStore bs) { return RestGuardList.create(bs).build(); }
	}

	@Test void a07_overrideMultiSegment() throws Exception {
		var key = "juneau.admin.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/api/v1/xxx/*");
		try {
			var c = MockRestClient.buildLax(A07_MultiSegment.class);
			c.get("/api/v1/xxx/threads").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json");
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
