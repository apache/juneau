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
package org.apache.juneau.rest.server.view.jsp;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Regression tests for path-traversal hardening in {@link JspMixin#render render(...)}.
 *
 * <p>
 * The pre-fix implementation concatenated the user-supplied {@code @Path("/*") String path}
 * onto the configured {@code basePath} via {@code JspViewRenderer.joinPath(...)} and passed the
 * result directly to {@link jakarta.servlet.ServletContext#getRequestDispatcher
 * ServletContext.getRequestDispatcher(...)} — so {@code ..} segments inside the {@code path}
 * could escape the configured base path and reach unrelated servlet-context resources (e.g.
 * {@code /WEB-INF/web.xml}). The post-fix implementation funnels {@code joinPath} through
 * {@link org.apache.juneau.commons.utils.FileUtils#resolveVirtualPathSafely
 * FileUtils.resolveVirtualPathSafely} and rejects any escape with HTTP 403 at the handler
 * boundary.
 *
 * <p>
 * MockRest does not URL-normalize request paths the way a real servlet container does, so
 * the {@code @Path("/*") String path} parameter receives the raw {@code ..} segments — which
 * is what the handler-layer boundary check is designed to catch. (Real-container coverage is
 * deferred to the same integration matrix that lights up Jetty/Spring Boot end-to-end
 * for the rest of the JSP module.)
 *
 * @since 10.0.0
 */
class JspMixin_PathTraversal_Test extends TestBase {

	@Rest(mixins=JspMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		// Register a non-default base path so the boundary check has somewhere to stay inside of.
		// (With the DEFAULT_BASE_PATH "/", every resolved target starts with "/" and the boundary
		// check vacuously passes — meaningful traversal coverage requires a non-root basePath.)
		@Bean
		public JspMixin jsp() {
			return JspMixin.create().basePath("/WEB-INF/views/").build();
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(A.class);

	//-----------------------------------------------------------------------------------------------------------------
	// Baseline — well-formed requests reach the dispatcher (no JSP engine on MockRest classpath
	// at the right place, so the engine-missing branch fires with a 500 + NO_ENGINE_DIAGNOSTIC).
	// A 200/500 here means the boundary check did NOT fire (correct for a non-traversing path).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t01_normalAccess_passesBoundaryCheck() throws Exception {
		// Request hits the boundary check, passes, and reaches the dispatcher. MockRest has no
		// JSP engine wired up, so the dispatcher returns null or forward fails → 500 with the
		// NO_ENGINE_DIAGNOSTIC. Not a 403 = the boundary check did not fire = correct.
		var status = c.get("/jsp/hello.jsp").run().getStatusCode();
		assertNotEquals(403, status, "Well-formed request must not be rejected by boundary check");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// CWE-22: direct .. traversal of basePath returns 403
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t02_directTraversal_returns403() throws Exception {
		var status = c.get("/jsp/../web.xml").run().getStatusCode();
		assertEquals(403, status, "GET /jsp/../web.xml must be rejected (escapes /WEB-INF/views/)");
	}

	@Test void t03_nestedTraversal_returns403() throws Exception {
		var status = c.get("/jsp/a/b/../../../web.xml").run().getStatusCode();
		assertEquals(403, status, "GET /jsp/a/b/../../../web.xml must be rejected");
	}

	@Test void t04_traversalToSibling_returns403() throws Exception {
		// /jsp/../views2/foo.jsp → after joinPath: /WEB-INF/views/../views2/foo.jsp →
		// normalized: /WEB-INF/views2/foo.jsp → does NOT start with /WEB-INF/views/ → 403.
		var status = c.get("/jsp/../views2/foo.jsp").run().getStatusCode();
		assertEquals(403, status, "Traversal to a sibling base-path-prefix must be rejected");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// URL-encoded traversal handling
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("java:S125") // Explanatory prose on URL-decoding behaviour; not dead code.
	@Test void t05_urlEncodedTraversal_doesNotEscape() throws Exception {
		// Behavior depends on whether HttpClient / MockRest URL-decodes %2e%2e before the
		// handler sees the path:
		//   - If decoded: the boundary check sees ".." and returns 403.
		//   - If NOT decoded: the boundary check sees the literal "%2e%2e" segment and passes;
		//     joinPath returns /WEB-INF/views/%2e%2e/web.xml; the dispatcher won't resolve to a
		//     real resource → 500 (no engine) or 404. Either way: not 200, not the outside file.
		var status = c.get("/jsp/%2e%2e/web.xml").run().getStatusCode();
		assertNotEquals(200, status,
			"URL-encoded traversal must not return 200. Status was: " + status);
	}
}
