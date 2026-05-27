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
package org.apache.juneau.rest.view.mustache;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.view.*;
import org.apache.juneau.rest.view.thymeleaf.*;
import org.junit.jupiter.api.*;

/**
 * Typed-handler View integration matrix for {@link MustacheViewRenderer}.
 *
 * <p>
 * Verifies that a host-class {@code @RestGet} method returning {@link MustacheView} reaches
 * {@link MustacheViewRenderer} (not {@code SerializedPojoProcessor}) when the renderer is
 * registered in the host's {@code @Rest(responseProcessors=...)}.
 *
 * <p>
 * mustache.java has zero servlet-container dependencies and renders directly to a
 * {@link java.io.Writer}.  Under MockRest the factory resolves templates from the test
 * classpath, so {@code a04} can assert actual interpolated output — body contains the
 * {@code name} attribute value passed via {@link MustacheView#attr(String, Object)}.
 *
 * <p>
 * The {@link MustacheViewRenderer} is mustache.java-specific.  Note that
 * {@code spring-boot-starter-mustache} ships {@code jmustache} (a different library); the
 * Spring Boot flavor requires an explicit {@code @Bean DefaultMustacheFactory} that wires
 * mustache.java.
 *
 * <h5 class='figure'>Template fixture used by a04:</h5>
 * <pre>
 *   src/test/resources/mustache-templates/hello.mustache  ←  &lt;p&gt;Hello, {{name}}!&lt;/p&gt;
 * </pre>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/MustacheViewSupport">Mustache View Support</a>
 * </ul>
 *
 * @since 9.5.0
 */
class MustacheView_TypedHandler_Test extends ViewIntegrationTestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// a01: MustacheView-returning @RestGet reaches MustacheViewRenderer (not SerializedPojoProcessor).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={MustacheViewRenderer.class})
	public static class A01_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			// Use a non-existent template; we only assert the renderer was reached.
			return MustacheView.of("__nonexistent__").attr("greeting", "Hello");
		}
	}

	/** MustacheView-returning @RestGet reaches MustacheViewRenderer — body is NOT a JSON bean dump. */
	@Test void a01_mustacheViewReturn_reachesRenderer() throws Exception {
		buildResource(A01_Host.class)
			.get("/hello")
			.accept("application/json")
			.run()
			.assertContent().asString().isNotContains("templateName");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a02: A plain String return on the same host falls through to SerializedPojoProcessor unchanged.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={MustacheViewRenderer.class})
	public static class A02_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/items")
		public String items() {
			return "items";
		}
	}

	/** String-returning @RestGet on a Mustache host still goes through SerializedPojoProcessor. */
	@Test void a02_fallThrough_nonViewReturn() throws Exception {
		buildResource(A02_Host.class)
			.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a03: Host with both MustacheViewRenderer and ThymeleafViewRenderer dispatches each View
	//      type to the correct renderer.  Neither engine crosses to the other.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={MustacheViewRenderer.class, ThymeleafViewRenderer.class})
	public static class A03_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/mu-hello")
		public View muHello() {
			return MustacheView.of("mustache-templates/hello.mustache").attr("name", "Alice");
		}

		@RestGet("/thy-hello")
		public View thyHello() {
			return ThymeleafView.of("templates/hello.html").attr("name", "Alice");
		}
	}

	/** MustacheView → MustacheViewRenderer ("Alice"); ThymeleafView → ThymeleafViewRenderer ("Alice"). */
	@Test void a03_multipleRenderers_correctDispatch() throws Exception {
		var c = buildResource(A03_Host.class);
		// MustacheView rendered by mustache.java.
		c.get("/mu-hello")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Alice");
		// ThymeleafView rendered by Thymeleaf engine — confirms no cross-dispatch.
		c.get("/thy-hello")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("Alice");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a04: Typed handler returns MustacheView with attributes.  mustache.java renders from classpath
	//      under MockRest, so we can assert actual attribute interpolation ({{name}} → "Alice").
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={MustacheViewRenderer.class})
	public static class A04_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello/{name}")
		public View hello(@Path String name) {
			return MustacheView.of("mustache-templates/hello.mustache").attr("name", name);
		}
	}

	/**
	 * Typed handler interpolates the {@code name} attribute via Mustache's {@code {{name}}} — body
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
	// a05: Host with NO renderer — MustacheView is bean-serialized by SerializedPojoProcessor.
	//      Response body DOES contain "templateName".
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A05_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			return MustacheView.of("mustache-templates/hello.mustache").attr("name", "Alice");
		}
	}

	/** No renderer in responseProcessors → MustacheView bean-serialized → body contains "templateName". */
	@Test void a05_noRenderer_fallsToSerializedPojo() throws Exception {
		buildResource(A05_Host.class)
			.get("/hello")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("templateName");
	}
}
