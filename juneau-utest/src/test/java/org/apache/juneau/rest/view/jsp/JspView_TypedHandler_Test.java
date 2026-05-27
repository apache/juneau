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
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.view.*;
import org.apache.juneau.rest.view.thymeleaf.*;
import org.junit.jupiter.api.*;

/**
 * Typed-handler View integration matrix for {@link JspViewRenderer}.
 *
 * <p>
 * Verifies that a host-class {@code @RestGet} method returning {@link JspView} reaches
 * {@link JspViewRenderer} (not {@code SerializedPojoProcessor}) when the renderer is registered
 * in the host's {@code @Rest(responseProcessors=...)}.
 *
 * <p>
 * JSP rendering requires a real servlet container with a JSP engine — MockRest provides neither.
 * Tests that assert rendering therefore verify only that the renderer was reached (the response
 * body does NOT contain the Juneau-bean JSON field {@code "templateName"} that would appear if
 * {@code SerializedPojoProcessor} ran instead).  The renderer may complete with 200 (empty body
 * when the dispatcher silently drops the forward) or 5xx (engine absent), both of which prove
 * the renderer was invoked.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JspViewSupport">JSP View Support</a>
 * </ul>
 *
 * @since 9.5.0
 */
class JspView_TypedHandler_Test extends ViewIntegrationTestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// a01: JspView-returning @RestGet reaches JspViewRenderer (not SerializedPojoProcessor).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={JspViewRenderer.class})
	public static class A01_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			return JspView.of("hello.jsp").attr("greeting", "Hello");
		}
	}

	/** JspView-returning @RestGet reaches JspViewRenderer — body is NOT a JSON bean dump of JspView. */
	@Test void a01_jspViewReturn_reachesRenderer() throws Exception {
		buildResource(A01_Host.class)
			.get("/hello")
			.accept("application/json")
			.run()
			.assertContent().asString().isNotContains("templateName");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a02: A plain String return on the same host falls through to SerializedPojoProcessor unchanged.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={JspViewRenderer.class})
	public static class A02_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/items")
		public String items() {
			return "items";
		}
	}

	/** String-returning @RestGet on a host with JspViewRenderer still goes through SerializedPojoProcessor. */
	@Test void a02_fallThrough_nonViewReturn() throws Exception {
		buildResource(A02_Host.class)
			.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a03: Host with both JspViewRenderer and ThymeleafViewRenderer dispatches each View type to the
	//      correct renderer.  JspView → JspViewRenderer (body NOT "templateName").
	//      ThymeleafView → ThymeleafViewRenderer (Thymeleaf renders from classpath → body contains "Alice").
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={JspViewRenderer.class, ThymeleafViewRenderer.class})
	public static class A03_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/jsp-hello")
		public View jspHello() {
			return JspView.of("hello.jsp").attr("name", "Alice");
		}

		@RestGet("/thy-hello")
		public View thyHello() {
			return ThymeleafView.of("templates/hello.html").attr("name", "Alice");
		}
	}

	/** JspView → JspViewRenderer (not bean-serialized); ThymeleafView → ThymeleafViewRenderer (renders "Alice"). */
	@Test void a03_multipleRenderers_correctDispatch() throws Exception {
		var c = buildResource(A03_Host.class);
		// JspView goes to JspViewRenderer — NOT a bean-JSON dump.
		c.get("/jsp-hello")
			.accept("application/json")
			.run()
			.assertContent().asString().isNotContains("templateName");
		// ThymeleafView goes to ThymeleafViewRenderer — Thymeleaf renders from classpath.
		c.get("/thy-hello")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Alice");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a04: Typed handler returns JspView with attributes set.  JSP requires a real servlet container so
	//      full rendering cannot be asserted under MockRest.  The key invariant is that the renderer is
	//      reached (body NOT "templateName") — attributes are passed to the renderer before the container
	//      call fails.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={JspViewRenderer.class})
	public static class A04_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello/{name}")
		public View hello(@Path String name) {
			return JspView.of("hello.jsp").attr("name", name);
		}
	}

	/**
	 * Typed handler returns JspView with attributes; renderer is reached (body NOT "templateName").
	 * Full JSP interpolation requires a real servlet container — see the JettyMicroservice / Spring Boot
	 * integration-test classes for end-to-end attribute substitution assertions.
	 */
	@Test void a04_renderInterpolatesAttributesFromTypedHandler() throws Exception {
		buildResource(A04_Host.class)
			.get("/hello/Alice")
			.accept("application/json")
			.run()
			.assertContent().asString().isNotContains("templateName");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a05: Host with NO renderer — JspView is bean-serialized by SerializedPojoProcessor.
	//      Response body DOES contain "templateName" (invariant holds in both directions).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A05_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			return JspView.of("hello.jsp").attr("greeting", "Hello");
		}
	}

	/** No renderer in responseProcessors → JspView bean-serialized → body contains "templateName". */
	@Test void a05_noRenderer_fallsToSerializedPojo() throws Exception {
		buildResource(A05_Host.class)
			.get("/hello")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("templateName");
	}
}
