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

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * MockRest-level assertions for {@link BasicFreemarkerResource} composition + route wiring.
 *
 * <p>
 * Like Thymeleaf and Mustache (and unlike JSP), the Apache FreeMarker engine has <b>zero
 * servlet-container dependencies</b> &mdash; it asks {@link freemarker.template.Configuration}
 * for a {@code Template} and {@code process(dataModel, writer)} writes directly to the response
 * writer. That makes the <i>raw-render path</i> (the mixin's own {@code /freemarker/*} handler
 * that calls {@code cfg.getTemplate(...).process(...)} directly onto the response writer) fully
 * exerciseable under MockRest, which is what this test covers.
 *
 * <h5 class='section'>Deferred &mdash; typed {@link org.apache.juneau.rest.view.View View} return
 * path:</h5>
 *
 * <p>
 * Verifying that an {@code @RestOp} method returning a {@link FreemarkerView} renders through
 * {@link FreemarkerViewRenderer} requires the renderer to run <b>before</b> the default
 * HTML/JSON/&hellip; serializer processors in the response chain. Today the mixin's
 * {@link Rest#responseProcessors() @Rest(responseProcessors=...)} declaration appends to the
 * chain, so the default HTML serializer wins under MockRest and the {@code FreemarkerView} is
 * serialized as a Juneau-bean HTML table instead of routed through FreeMarker. The response-
 * processor "prepend" mechanism that fixes this is tracked separately (work item 96) and blocks
 * the typed-handler integration matrix for all four sibling view modules (JSP / Thymeleaf /
 * Mustache / FreeMarker). The typed-handler test surface lights up alongside the real-container
 * coverage once that prereq lands &mdash; tracked as the FreeMarker analog of the deferred JSP /
 * Thymeleaf / Mustache follow-ons.
 *
 * <h5 class='figure'>Test resource layout (classpath {@code src/test/resources}):</h5>
 *
 * <pre>
 *   /freemarker-templates/hello.ftlh        ← greets "Hello, ${name}!" (used by future typed-handler tests)
 *   /freemarker-templates/about.ftlh        ← static "About Juneau"
 *   /freemarker-templates/nested/inner.ftlh ← validates multi-segment paths
 * </pre>
 *
 * @since 9.5.0
 */
class BasicFreemarkerResource_MockRest_Test extends TestBase {

	/* ---------------------------------------------------------------------------------------- *
	 * Resource A: mixin only — default base path "/" + default configuration + no implicit suffix
	 * ---------------------------------------------------------------------------------------- */

	@Rest(mixins=BasicFreemarkerResource.class)
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

	@Test void a02_freemarkerMountInstalled_rendersTemplate() throws Exception {
		// Default basePath="/" + no templateSuffix → request path is taken as-is, FreeMarker's
		// root-of-classpath loader resolves it directly.
		ca.get("/freemarker/freemarker-templates/about.ftlh")
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
	 * Resource B: configured base path "/freemarker-templates/" + templateSuffix ".ftlh"
	 * ---------------------------------------------------------------------------------------- */

	@Rest(mixins=BasicFreemarkerResource.class)
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public BasicFreemarkerResource freemarker() {
			return BasicFreemarkerResource.create()
				.basePath("/freemarker-templates/")
				.templateSuffix(".ftlh")
				.build();
		}
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_renderResolvesUnderConfiguredBasePath() throws Exception {
		// basePath = /freemarker-templates/, suffix = .ftlh.
		// /freemarker/about → cfg.getTemplate("about.ftlh") → resolves
		// /freemarker-templates/about.ftlh on the classpath.
		cb.get("/freemarker/about")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("About Juneau");
	}

	@Test void b02_renderResolvesWithExplicitSuffix() throws Exception {
		// Same template via the explicit-suffix request form; suffix is idempotent.
		cb.get("/freemarker/about.ftlh")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("About Juneau");
	}

	@Test void b03_renderHandlesMultiSegmentPath() throws Exception {
		cb.get("/freemarker/nested/inner")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("inner template");
	}

	@Test void b04_contentTypeDefaultsToHtml() throws Exception {
		cb.get("/freemarker/about")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("text/html");
	}
}
