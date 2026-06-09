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
package org.apache.juneau.rest.server.view;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.processor.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.view.jsp.*;
import org.apache.juneau.rest.server.view.mustache.*;
import org.apache.juneau.rest.server.view.thymeleaf.*;
import org.junit.jupiter.api.*;

/**
 * Marker-conformance and end-to-end ordering tests for {@link ViewRenderer}.
 *
 * <p>
 * The marker-conformance assertions verify that all four built-in renderers implement
 * {@link ViewRenderer}, making them eligible for the partition pass in
 * {@link ResponseProcessorList}.
 *
 * <p>
 * The end-to-end assertions use {@link MockRestClient} to verify that a {@code @RestGet} method
 * returning a typed {@code View} subclass reaches the corresponding renderer rather than being
 * serialized by {@link SerializedPojoProcessor}.  MockRest does not provide a JSP engine or
 * real template files for the renderer to dispatch to, so the renderer fails with a 500 — but
 * a 500 from the renderer is categorically different from a 200 with Juneau-bean HTML content
 * that would appear if the catch-all serializer ran first.
 *
 * @since 10.0.0
 */
class ViewRenderer_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// a: Marker-conformance — all four built-in renderers implement ViewRenderer.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_jspViewRenderer_implementsViewRenderer() {
		assertInstanceOf(ViewRenderer.class, new JspViewRenderer());
	}

	@Test void a02_thymeleafViewRenderer_implementsViewRenderer() {
		assertInstanceOf(ViewRenderer.class, new ThymeleafViewRenderer());
	}

	@Test void a03_mustacheViewRenderer_implementsViewRenderer() {
		assertInstanceOf(ViewRenderer.class, new MustacheViewRenderer());
	}

	@Test void a04_freemarkerViewRenderer_implementsViewRenderer() {
		assertInstanceOf(ViewRenderer.class, new FreemarkerViewRenderer());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// b: ViewRenderer also extends ResponseProcessor (contract via marker chain).
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_viewRenderer_extendsResponseProcessor() {
		assertInstanceOf(ResponseProcessor.class, new JspViewRenderer());
		assertInstanceOf(ResponseProcessor.class, new ThymeleafViewRenderer());
		assertInstanceOf(ResponseProcessor.class, new MustacheViewRenderer());
		assertInstanceOf(ResponseProcessor.class, new FreemarkerViewRenderer());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// c: End-to-end ordering: JspView-returning @RestGet reaches JspViewRenderer (not SerializedPojoProcessor).
	//
	// The partition pass in ResponseProcessorList reorders any ViewRenderer before any
	// CatchAllResponseProcessor in the same list.  In C01_JspViewResource the host resource explicitly
	// adds JspViewRenderer.class to responseProcessors — without the partition pass it would land after
	// SerializedPojoProcessor (which is contributed by DefaultConfig) and the JspView object would be
	// bean-serialized to {"templateName":"hello.jsp",...}.  With the partition pass, JspViewRenderer runs
	// first.  In MockRest the dispatcher may silently complete (200 empty body) or throw (5xx), but
	// either way the response body must NOT contain "templateName" as a serialized-bean field.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={JspViewRenderer.class})
	public static class C01_JspViewResource extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			return JspView.of("hello.jsp").attr("greeting", "Hello");
		}
	}

	/** JspView-returning @RestGet reaches JspViewRenderer first (partition pass) — response body is NOT
	 * a JSON bean dump of JspView (which would contain "templateName" if SerializedPojoProcessor ran). */
	@Test void c01_jspView_reachesRenderer_notBeanHtml() throws Exception {
		var c = MockRestClient.buildLax(C01_JspViewResource.class);
		c.get("/hello")
			.accept("application/json")
			.run()
			.assertContent().asString().isNotContains("templateName");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// d: End-to-end ordering for ThymeleafView.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(responseProcessors={ThymeleafViewRenderer.class})
	public static class D01_ThymeleafViewResource extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet("/hello")
		public View hello() {
			// Use a template name that doesn't exist on the classpath so the renderer fails.
			return ThymeleafView.of("__nonexistent_todo96_template__").attr("greeting", "Hello");
		}
	}

	/** ThymeleafView-returning @RestGet reaches ThymeleafViewRenderer first (partition pass) — response
	 * body is NOT a JSON bean dump of ThymeleafView (which would contain "templateName" if
	 * SerializedPojoProcessor ran). */
	@Test void d01_thymeleafView_reachesRenderer_notBeanHtml() throws Exception {
		var c = MockRestClient.buildLax(D01_ThymeleafViewResource.class);
		c.get("/hello")
			.accept("application/json")
			.run()
			.assertContent().asString().isNotContains("templateName");
	}
}
