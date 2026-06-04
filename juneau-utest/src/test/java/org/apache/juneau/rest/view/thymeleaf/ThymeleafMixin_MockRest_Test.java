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
package org.apache.juneau.rest.view.thymeleaf;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * MockRest-level assertions for {@link ThymeleafMixin} composition + route wiring.
 *
 * <p>
 * Unlike the JSP module, Thymeleaf's core engine has <b>zero servlet-container dependencies</b>
 * &mdash; it renders directly to a {@link java.io.Writer Writer}. That makes the
 * <i>raw-render path</i> (the mixin's own {@code /thymeleaf/*} handler that calls
 * {@code engine.process(...)} directly onto the response writer) fully exerciseable under
 * MockRest, which is what this test covers.
 *
 * <h5 class='section'>Deferred &mdash; typed {@link org.apache.juneau.rest.view.View View} return
 * path:</h5>
 *
 * <h5 class='figure'>Test resource layout (classpath {@code src/test/resources}):</h5>
 *
 * <pre>
 *   /templates/hello.html       ← greets "Hello, [[${name}]]!" (used by future typed-handler tests)
 *   /templates/about.html       ← static "About Juneau"
 *   /templates/nested/inner.html ← validates multi-segment paths
 * </pre>
 *
 * @since 10.0.0
 */
class ThymeleafMixin_MockRest_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Resource A: mixin only — default base path "/" + default engine
	 * ---------------------------------------------------------------------------------------- */

	@Rest(mixins=ThymeleafMixin.class)
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_hostEndpointStillReachable() throws Exception {
		ca.get("/items")
			.accept("application/json")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("items");
	}

	@Test void a02_thymeleafMountInstalled_rendersTemplate() throws Exception {
		ca.get("/thymeleaf/templates/about")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("About Juneau");
	}

	@Test void a03_thymeleafMountInstalled_rendersDotHtml() throws Exception {
		// Trailing .html is stripped by the handler; both work.
		ca.get("/thymeleaf/templates/about.html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("About Juneau");
	}

	@Test void a05_nonMixinPathFallsThrough() throws Exception {
		ca.get("/does-not-exist")
			.run()
			.assertStatus(404);
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Resource B: configured base path "/templates/" — raw render uses engine-relative names
	 * ---------------------------------------------------------------------------------------- */

	@Rest(mixins=ThymeleafMixin.class)
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public ThymeleafMixin thymeleaf() {
			return ThymeleafMixin.create()
				.basePath("/templates/")
				.cacheTemplates(false)
				.build();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_renderResolvesUnderConfiguredBasePath() throws Exception {
		// basePath = /templates/, so /thymeleaf/about → engine.process("about", ...) →
		// resolver looks up /templates/about.html on the classpath.
		cb.get("/thymeleaf/about")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("About Juneau");
	}

	@Test void b02_renderHandlesMultiSegmentPath() throws Exception {
		cb.get("/thymeleaf/nested/inner.html")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("inner template");
	}

	@Test void b03_contentTypeDefaultsToHtml() throws Exception {
		cb.get("/thymeleaf/about")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/html");
	}
}
