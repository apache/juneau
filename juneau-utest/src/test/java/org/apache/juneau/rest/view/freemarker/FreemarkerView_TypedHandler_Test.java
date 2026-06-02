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

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.rest.view.*;
import org.apache.juneau.rest.view.thymeleaf.*;
import org.junit.jupiter.api.*;

/**
 * Typed-handler View integration matrix for {@link FreemarkerViewRenderer}.
 *
 * <p>
 * Verifies that a host-class {@code @RestGet} method returning {@link FreemarkerView} reaches
 * {@link FreemarkerViewRenderer} (not {@code SerializedPojoProcessor}) when the renderer is
 * registered in the host's {@code @Rest(responseProcessors=...)}.
 *
 * <p>
 * Apache FreeMarker has zero servlet-container dependencies and renders directly to a
 * {@link java.io.Writer}.  Under MockRest the engine resolves templates from the test classpath
 * via its default classpath-root {@link freemarker.template.Configuration}, so {@code a04} can
 * assert the actual interpolated output — body contains the {@code name} attribute value passed
 * via {@link FreemarkerView#attr(String, Object)}.
 *
 * <p>
 * Spring Boot's {@code spring-boot-starter-freemarker} autoconfigures a
 * {@link freemarker.template.Configuration} bean that the bridge picks up automatically via
 * {@code BeanStore.getBean(Configuration.class)} — the cleanest of the four Spring Boot view
 * stories.  That end-to-end path is exercised in the Spring Boot integration-test class.
 *
 * <h5 class='figure'>Template fixture used by a04:</h5>
 * <pre>
 *   src/test/resources/freemarker-templates/hello.ftlh  ←  &lt;p&gt;Hello, ${name}!&lt;/p&gt;
 * </pre>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/FreemarkerViewSupport">FreeMarker View Support</a>
 * </ul>
 *
 * @since 9.5.0
 */
class FreemarkerView_TypedHandler_Test extends ViewIntegrationTestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// a01: FreemarkerView-returning @RestGet reaches FreemarkerViewRenderer (not SerializedPojoProcessor).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={FreemarkerViewRenderer.class})
	public static class A01_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			// Use a non-existent template; we only assert the renderer was reached.
			return FreemarkerView.of("__nonexistent__").attr("greeting", "Hello");
		}
	}

	/** FreemarkerView-returning @RestGet reaches FreemarkerViewRenderer — body is NOT a JSON bean dump. */
	@Test void a01_freemarkerViewReturn_reachesRenderer() throws Exception {
		buildResource(A01_Host.class)
			.get("/hello")
			.accept("application/json")
			.run()
			.assertContent().asString().isNotContains("templateName");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a02: A plain String return on the same host falls through to SerializedPojoProcessor unchanged.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={FreemarkerViewRenderer.class})
	public static class A02_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/items")
		public String items() {
			return "items";
		}
	}

	/** String-returning @RestGet on a FreeMarker host still goes through SerializedPojoProcessor. */
	@Test void a02_fallThrough_nonViewReturn() throws Exception {
		buildResource(A02_Host.class)
			.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a03: Host with both FreemarkerViewRenderer and ThymeleafViewRenderer dispatches each View
	//      type to the correct renderer.  Neither engine crosses to the other.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={FreemarkerViewRenderer.class, ThymeleafViewRenderer.class})
	public static class A03_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/fm-hello")
		public View fmHello() {
			return FreemarkerView.of("freemarker-templates/hello.ftlh").attr("name", "Alice");
		}

		@RestGet("/thy-hello")
		public View thyHello() {
			return ThymeleafView.of("templates/hello.html").attr("name", "Alice");
		}
	}

	/** FreemarkerView → FreemarkerViewRenderer ("Alice"); ThymeleafView → ThymeleafViewRenderer ("Alice"). */
	@Test void a03_multipleRenderers_correctDispatch() throws Exception {
		var c = buildResource(A03_Host.class);
		// FreemarkerView rendered by FreeMarker engine.
		c.get("/fm-hello")
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
	// a04: Typed handler returns FreemarkerView with attributes.  FreeMarker renders from classpath
	//      under MockRest, so we can assert actual attribute interpolation (${name} → "Alice").
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={FreemarkerViewRenderer.class})
	public static class A04_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello/{name}")
		public View hello(@Path String name) {
			return FreemarkerView.of("freemarker-templates/hello.ftlh").attr("name", name);
		}
	}

	/**
	 * Typed handler interpolates the {@code name} attribute via FreeMarker's {@code ${name}} — body
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
	// a05: Host with NO renderer — FreemarkerView is bean-serialized by SerializedPojoProcessor.
	//      Response body DOES contain "templateName".
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A05_Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			return FreemarkerView.of("freemarker-templates/hello.ftlh").attr("name", "Alice");
		}
	}

	/** No renderer in responseProcessors → FreemarkerView bean-serialized → body contains "templateName". */
	@Test void a05_noRenderer_fallsToSerializedPojo() throws Exception {
		buildResource(A05_Host.class)
			.get("/hello")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("templateName");
	}
}
