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
package org.apache.juneau.rest.view.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Regression tests for path-traversal hardening in
 * {@link FreemarkerMixin#render render(...)}.
 *
 * <p>
 * Mirrors {@code JspMixin_PathTraversal_Test},
 * {@code ThymeleafMixin_PathTraversal_Test}, and
 * {@code MustacheMixin_PathTraversal_Test}: the handler must reject any user-supplied
 * {@code @Path("/*") String path} that resolves outside the configured {@code basePath} with HTTP
 * 403, via {@link org.apache.juneau.commons.utils.FileUtils#resolveVirtualPathSafely
 * FileUtils.resolveVirtualPathSafely}.
 *
 * <p>
 * MockRest does not URL-normalize request paths the way a real servlet container does, so
 * {@code @Path("/*") String path} receives the raw {@code ..} segments &mdash; which is what the
 * handler-layer boundary check is designed to catch. (Real-container coverage is deferred to the
 *
 * @since 10.0.0
 */
class FreemarkerMixin_PathTraversal_Test extends TestBase {

	@Rest(mixins=FreemarkerMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		// Register a non-default base path so the boundary check has somewhere to stay inside of.
		@Bean
		public FreemarkerMixin freemarker() {
			return FreemarkerMixin.create()
				.basePath("/freemarker-templates/")
				.templateSuffix(".ftlh")
				.build();
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	//-----------------------------------------------------------------------------------------------------------------
	// Baseline — well-formed requests reach the engine (real template renders since FreeMarker
	// works under MockRest, so we expect 200, not 403).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t01_normalAccess_passesBoundaryCheck() throws Exception {
		var status = c.get("/freemarker/about").run().getStatusCode();
		assertNotEquals(403, status, "Well-formed request must not be rejected by boundary check");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// CWE-22: direct .. traversal of basePath returns 403
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t02_directTraversal_returns403() throws Exception {
		var status = c.get("/freemarker/../secret").run().getStatusCode();
		assertEquals(403, status,
			"GET /freemarker/../secret must be rejected (escapes /freemarker-templates/)");
	}

	@Test void t03_nestedTraversal_returns403() throws Exception {
		var status = c.get("/freemarker/a/b/../../../secret").run().getStatusCode();
		assertEquals(403, status, "GET /freemarker/a/b/../../../secret must be rejected");
	}

	@Test void t04_traversalToSibling_returns403() throws Exception {
		// /freemarker/../views2/foo → after join: /freemarker-templates/../views2/foo →
		// normalized: /views2/foo → does NOT start with /freemarker-templates/ → 403.
		var status = c.get("/freemarker/../views2/foo").run().getStatusCode();
		assertEquals(403, status, "Traversal to a sibling base-path-prefix must be rejected");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// URL-encoded traversal handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t05_urlEncodedTraversal_doesNotEscape() throws Exception {
		// Behavior depends on whether HttpClient / MockRest URL-decodes %2e%2e before the
		// handler sees the path. Either way, the response MUST NOT be 200 — that would mean the
		// outside file got rendered.
		var status = c.get("/freemarker/%2e%2e/secret").run().getStatusCode();
		assertNotEquals(200, status,
			"URL-encoded traversal must not return 200. Status was: " + status);
	}
}
