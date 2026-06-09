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
package org.apache.juneau.rest.server.view.thymeleaf;

import org.apache.juneau.http.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.junit.jupiter.api.*;

/**
 * Typed-handler View integration matrix for {@link ThymeleafViewRenderer}.
 *
 * <p>
 * Verifies that a host-class {@code @RestGet} method returning {@link ThymeleafView} reaches
 * {@link ThymeleafViewRenderer} (not {@code SerializedPojoProcessor}) when the renderer is
 * registered in the host's {@code @Rest(responseProcessors=...)}.
 *
 * <p>
 * Unlike JSP, Thymeleaf's core engine has zero servlet-container dependencies and renders
 * directly to a {@link java.io.Writer}.  Under MockRest the engine resolves templates from the
 * test classpath, so {@code a04} can assert the actual interpolated output — body contains the
 * {@code name} attribute value passed via {@link ThymeleafView#attr(String, Object)}.
 *
 * <h5 class='figure'>Template fixture used by a04:</h5>
 * <pre>
 *   src/test/resources/templates/hello.html  ←  &lt;span th:text="${name}"&gt;...&lt;/span&gt;
 * </pre>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ThymeleafViewSupport">Thymeleaf View Support</a>
 * </ul>
 *
 * @since 10.0.0
 */
class ThymeleafView_TypedHandler_Test extends ViewIntegrationTestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// a01: ThymeleafView-returning @RestGet reaches ThymeleafViewRenderer (not SerializedPojoProcessor).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={ThymeleafViewRenderer.class})
	public static class A01_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			// Use a non-existent template; we only assert the renderer was reached.
			return ThymeleafView.of("__nonexistent__").attr("greeting", "Hello");
		}
	}

	/** ThymeleafView-returning @RestGet reaches ThymeleafViewRenderer — body is NOT a JSON bean dump. */
	@Test void a01_thymeleafViewReturn_reachesRenderer() throws Exception {
		buildResource(A01_Host.class)
			.get("/hello")
			.accept("application/json")
			.run()
			.assertContent().asString().isNotContains("templateName");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a02: A plain String return on the same host falls through to SerializedPojoProcessor unchanged.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={ThymeleafViewRenderer.class})
	public static class A02_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/items")
		public String items() {
			return "items";
		}
	}

	/** String-returning @RestGet on a Thymeleaf host still goes through SerializedPojoProcessor. */
	@Test void a02_fallThrough_nonViewReturn() throws Exception {
		buildResource(A02_Host.class)
			.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a03: Host with both ThymeleafViewRenderer and FreemarkerViewRenderer dispatches each View
	//      type to the correct renderer.  Neither engine crosses to the other.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={ThymeleafViewRenderer.class, FreemarkerViewRenderer.class})
	public static class A03_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/thy-hello")
		public View thyHello() {
			return ThymeleafView.of("templates/hello.html").attr("name", "Alice");
		}

		@RestGet("/fm-hello")
		public View fmHello() {
			return FreemarkerView.of("freemarker-templates/hello.ftlh").attr("name", "Alice");
		}
	}

	/** ThymeleafView → ThymeleafViewRenderer ("Alice"); FreemarkerView → FreemarkerViewRenderer ("Alice"). */
	@Test void a03_multipleRenderers_correctDispatch() throws Exception {
		var c = buildResource(A03_Host.class);
		// ThymeleafView rendered by Thymeleaf engine.
		c.get("/thy-hello")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Alice");
		// FreemarkerView rendered by FreeMarker engine.
		c.get("/fm-hello")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Alice");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a04: Typed handler returns ThymeleafView with attributes.  Thymeleaf renders from classpath
	//      under MockRest, so we can assert actual attribute interpolation ("Alice" in the response body).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={ThymeleafViewRenderer.class})
	public static class A04_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello/{name}")
		public View hello(@Path String name) {
			return ThymeleafView.of("templates/hello.html").attr("name", name);
		}
	}

	/**
	 * Typed handler interpolates the {@code name} attribute via Thymeleaf's {@code th:text} — body
	 * contains the actual name value ("Alice"), not a bean-JSON dump.
	 */
	@Test void a04_renderInterpolatesAttributesFromTypedHandler() throws Exception {
		buildResource(A04_Host.class)
			.get("/hello/Alice")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Alice")
			.assertContent().asString().isNotContains("templateName");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a05: Host with NO renderer — ThymeleafView is bean-serialized by SerializedPojoProcessor.
	//      Response body DOES contain "templateName".
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A05_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			return ThymeleafView.of("templates/hello.html").attr("name", "Alice");
		}
	}

	/** No renderer in responseProcessors → ThymeleafView bean-serialized → body contains "templateName". */
	@Test void a05_noRenderer_fallsToSerializedPojo() throws Exception {
		buildResource(A05_Host.class)
			.get("/hello")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("templateName");
	}
}
