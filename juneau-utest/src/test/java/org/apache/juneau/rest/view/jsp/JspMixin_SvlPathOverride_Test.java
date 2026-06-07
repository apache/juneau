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
package org.apache.juneau.rest.view.jsp;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@code ${juneau.jsp.path:jsp}} SVL override on {@link JspMixin}.
 *
 * <p>
 * MockRest does not provide a JSP engine, so the renderer surfaces {@code 500} when the route
 * is matched (vs. {@code 404} when the URL doesn't match the configured prefix). This test
 * uses that distinction to verify that the SVL substitution moved the mount point.
 *
 * <p>
 * Uses a fresh inner-class resource because {@link MockRestClient} caches {@link RestContext}
 * per resource class &mdash; SVL substitution is captured at context-construction time.
 *
 * @since 10.0.0
 */
class JspMixin_SvlPathOverride_Test extends TestBase {

	@Rest(mixins=JspMixin.class)
	public static class A01_OverridePath extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_svlOverrideChangesPath() throws Exception {
		var key = "juneau.jsp.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "views");
		try {
			var c = MockRestClient.buildLax(A01_OverridePath.class);

			// Default /jsp/* is no longer mounted.
			c.get("/jsp/anything.jsp").run().assertStatus(404);

			// Overridden /views/* is now mounted (500 because no JSP engine on MockRest classpath).
			c.get("/views/anything.jsp").run().assertStatus(500);
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=JspMixin.class)
	public static class A02_BareToken extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a02_overrideBareToken() throws Exception {
		var key = "juneau.jsp.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx");
		try {
			var c = MockRestClient.buildLax(A02_BareToken.class);
			c.get("/xxx/test.jsp").run().assertStatus(500);
			c.get("/jsp/test.jsp").run().assertStatus(404);
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=JspMixin.class)
	public static class A03_LeadingSlash extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03_overrideLeadingSlash() throws Exception {
		var key = "juneau.jsp.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx");
		try {
			var c = MockRestClient.buildLax(A03_LeadingSlash.class);
			c.get("/xxx/test.jsp").run().assertStatus(500);
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=JspMixin.class)
	public static class A04_TrailingSlash extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a04_overrideTrailingSlash() throws Exception {
		var key = "juneau.jsp.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "xxx/");
		try {
			var c = MockRestClient.buildLax(A04_TrailingSlash.class);
			c.get("/xxx/test.jsp").run().assertStatus(500);
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=JspMixin.class)
	public static class A05_BothSlashes extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a05_overrideBothSlashes() throws Exception {
		var key = "juneau.jsp.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/");
		try {
			var c = MockRestClient.buildLax(A05_BothSlashes.class);
			c.get("/xxx/test.jsp").run().assertStatus(500);
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=JspMixin.class)
	public static class A06_WildcardSuffix extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a06_overrideWildcardSuffix() throws Exception {
		var key = "juneau.jsp.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/xxx/*");
		try {
			var c = MockRestClient.buildLax(A06_WildcardSuffix.class);
			c.get("/xxx/test.jsp").run().assertStatus(500);
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}

	@Rest(mixins=JspMixin.class)
	public static class A07_MultiSegment extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a07_overrideMultiSegment() throws Exception {
		var key = "juneau.jsp.path";
		var prev = System.getProperty(key);
		System.setProperty(key, "/api/v1/xxx/*");
		try {
			var c = MockRestClient.buildLax(A07_MultiSegment.class);
			c.get("/api/v1/xxx/test.jsp").run().assertStatus(500);
		} finally {
			if (prev == null) System.clearProperty(key);
			else System.setProperty(key, prev);
		}
	}
}
