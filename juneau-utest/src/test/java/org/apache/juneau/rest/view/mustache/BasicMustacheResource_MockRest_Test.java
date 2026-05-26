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
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * MockRest-level assertions for {@link BasicMustacheResource} composition + route wiring.
 *
 * <p>
 * Like Thymeleaf and unlike JSP, mustache.java has <b>zero servlet-container dependencies</b>
 * &mdash; it compiles a template into a {@code Mustache} object and asks it to
 * {@code execute(Writer, scope)} directly. That makes the <i>raw-render path</i> (the mixin's
 * own {@code /mustache/*} handler that calls {@code factory.compile(...).execute(...)} directly
 * onto the response writer) fully exerciseable under MockRest, which is what this test covers.
 *
 * <h5 class='section'>Deferred &mdash; typed {@link org.apache.juneau.rest.view.View View} return
 * path:</h5>
 *
 * <p>
 * Verifying that an {@code @RestOp} method returning a {@link MustacheView} renders through
 * {@link MustacheViewRenderer} requires the renderer to run <b>before</b> the default
 * HTML/JSON/&hellip; serializer processors in the response chain. Today the mixin's
 * {@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration appends to the
 * chain, so the default HTML serializer wins under MockRest and the {@code MustacheView} is
 * serialized as a Juneau-bean HTML table instead of routed through Mustache. The response-
 * processor "prepend" mechanism that fixes this is tracked separately (TODO-96) and blocks the
 * typed-handler integration matrix for all three sibling view modules (JSP / Thymeleaf /
 * Mustache). The typed-handler test surface lights up alongside the real-container coverage
 * once TODO-96 lands &mdash; tracked as the Mustache analog of TODO-97 (JSP) and TODO-107
 * (Thymeleaf).
 *
 * <h5 class='figure'>Test resource layout (classpath {@code src/test/resources}):</h5>
 *
 * <pre>
 *   /mustache-templates/hello.mustache       ← greets "Hello, {{name}}!" (used by future typed-handler tests)
 *   /mustache-templates/about.mustache       ← static "About Juneau"
 *   /mustache-templates/nested/inner.mustache ← validates multi-segment paths
 * </pre>
 *
 * @since 9.5.0
 */
class BasicMustacheResource_MockRest_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Resource A: mixin only — default base path "/" + default factory + no implicit suffix
	 * ---------------------------------------------------------------------------------------- */

	@Rest(mixins=BasicMustacheResource.class)
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

	@Test void a02_mustacheMountInstalled_rendersTemplate() throws Exception {
		// Default basePath="/" + no templateSuffix → request path is taken as-is, mustache.java's
		// no-prefix factory resolves it directly from classpath.
		ca.get("/mustache/mustache-templates/about.mustache")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("About Juneau");
	}

	@Test void a04_nonMixinPathFallsThrough() throws Exception {
		ca.get("/does-not-exist")
			.run()
			.assertStatus(404);
	}

	/* ---------------------------------------------------------------------------------------- *
	 * Resource B: configured base path "/mustache-templates/" + templateSuffix ".mustache"
	 * ---------------------------------------------------------------------------------------- */

	@Rest(mixins=BasicMustacheResource.class)
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public BasicMustacheResource mustache() {
			return BasicMustacheResource.create()
				.basePath("/mustache-templates/")
				.templateSuffix(".mustache")
				.build();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_renderResolvesUnderConfiguredBasePath() throws Exception {
		// basePath = /mustache-templates/, suffix = .mustache.
		// /mustache/about → factory.compile("about.mustache") → resolves
		// mustache-templates/about.mustache on the classpath.
		cb.get("/mustache/about")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("About Juneau");
	}

	@Test void b02_renderResolvesWithExplicitSuffix() throws Exception {
		// Same template via the explicit-suffix request form; suffix is idempotent.
		cb.get("/mustache/about.mustache")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("About Juneau");
	}

	@Test void b03_renderHandlesMultiSegmentPath() throws Exception {
		cb.get("/mustache/nested/inner")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("inner template");
	}

	@Test void b04_contentTypeDefaultsToHtml() throws Exception {
		cb.get("/mustache/about")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/html");
	}
}
